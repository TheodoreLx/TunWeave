package io.github.theodorelx.tunweave.util

import io.github.theodorelx.tunweave.data.ProxyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets

object LatencyTester {

    private const val TAG = "LatencyTester"
    const val DEFAULT_TEST_URL = "https://www.gstatic.com/generate_204"
    const val TIMEOUT_MS = 5000

    /**
     * Complete a SOCKS5 handshake and CONNECT request to the configured test target.
     * Returns the elapsed time in milliseconds, or -1 if the proxy is unavailable.
     */
    suspend fun testProxyLatency(config: ProxyConfig): Long = withContext(Dispatchers.IO) {
        if (config.proxyHost.isBlank() || config.proxyPort <= 0) {
            AppLogger.w(TAG, "跳过测延迟: 代理地址或端口未设置 (${config.proxyHost}:${config.proxyPort})")
            return@withContext -1L
        }

        val testUrlStr = config.latencyTestUrl.ifBlank { DEFAULT_TEST_URL }
        val startTime = System.currentTimeMillis()
        try {
            AppLogger.i(TAG, "开始测试 SOCKS5 代理延迟 -> ${config.proxyHost}:${config.proxyPort} (目标: $testUrlStr)")
            val url = URL(testUrlStr)
            val targetHost = requireNotNull(url.host.takeIf { it.isNotBlank() }) {
                "延迟测试 URL 缺少主机名"
            }
            val targetPort = if (url.port > 0) url.port else url.defaultPort.takeIf { it > 0 } ?: 443

            Socket().use { socket ->
                socket.connect(InetSocketAddress(config.proxyHost, config.proxyPort), TIMEOUT_MS)
                socket.soTimeout = TIMEOUT_MS
                val input = DataInputStream(socket.getInputStream())
                val output = DataOutputStream(socket.getOutputStream())

                negotiateAuthentication(input, output, config)
                requestConnect(input, output, targetHost, targetPort)

                val elapsedTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                AppLogger.i(TAG, "SOCKS5 握手成功，耗时 ${elapsedTime}ms")
                elapsedTime
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "测延迟失败: ${e.message}", e)
            -1L
        }
    }

    private fun negotiateAuthentication(
        input: DataInputStream,
        output: DataOutputStream,
        config: ProxyConfig,
    ) {
        val hasCredentials = config.username.isNotEmpty()
        output.write(if (hasCredentials) byteArrayOf(5, 2, 0, 2) else byteArrayOf(5, 1, 0))
        output.flush()

        val version = input.readUnsignedByte()
        val method = input.readUnsignedByte()
        require(version == 5 && method != 0xFF) { "SOCKS5 服务拒绝认证方式" }

        if (method == 2) {
            val username = config.username.toByteArray(StandardCharsets.UTF_8)
            val password = config.password.toByteArray(StandardCharsets.UTF_8)
            require(username.isNotEmpty() && username.size <= 255 && password.size <= 255) {
                "SOCKS5 用户名或密码长度无效"
            }
            output.writeByte(1)
            output.writeByte(username.size)
            output.write(username)
            output.writeByte(password.size)
            output.write(password)
            output.flush()
            require(input.readUnsignedByte() == 1 && input.readUnsignedByte() == 0) {
                "SOCKS5 用户名或密码错误"
            }
        } else {
            require(method == 0) { "SOCKS5 服务返回未知认证方式: $method" }
        }
    }

    private fun requestConnect(
        input: DataInputStream,
        output: DataOutputStream,
        host: String,
        port: Int,
    ) {
        val domain = host.toByteArray(StandardCharsets.UTF_8)
        require(domain.isNotEmpty() && domain.size <= 255) { "目标主机名长度无效" }

        output.write(byteArrayOf(5, 1, 0, 3))
        output.writeByte(domain.size)
        output.write(domain)
        output.writeShort(port)
        output.flush()

        require(input.readUnsignedByte() == 5) { "无效的 SOCKS5 响应版本" }
        val reply = input.readUnsignedByte()
        input.readUnsignedByte() // reserved
        val addressType = input.readUnsignedByte()
        require(reply == 0) { "SOCKS5 CONNECT 失败，响应码: $reply" }
        when (addressType) {
            1 -> input.skipFully(4)
            3 -> input.skipFully(input.readUnsignedByte())
            4 -> input.skipFully(16)
            else -> error("未知的 SOCKS5 地址类型: $addressType")
        }
        input.skipFully(2)
    }

    private fun DataInputStream.skipFully(length: Int) {
        val bytes = ByteArray(length)
        readFully(bytes)
    }
}
