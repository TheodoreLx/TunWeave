package io.github.theodorelx.tunweave.tun2socks

import android.content.Context
import com.simpleproxy.tun2socks.Tun2socksJni
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.data.MAP_DNS_ADDRESS
import io.github.theodorelx.tunweave.data.MAP_DNS_CACHE_SIZE
import io.github.theodorelx.tunweave.data.MAP_DNS_NETMASK
import io.github.theodorelx.tunweave.data.MAP_DNS_NETWORK
import io.github.theodorelx.tunweave.data.ProxyConfig
import io.github.theodorelx.tunweave.util.AppLogger
import java.io.File

/**
 * Native tun2socks bridge and engine manager.
 * Interoperates with native libhev-socks5-tunnel.so.
 */
interface ITun2socks {
    fun start(context: Context, tunFd: Int, config: ProxyConfig): Boolean
    fun stop(): Boolean
    fun isRunning(): Boolean
    fun getTrafficStats(): Tun2socksTrafficStats?
}

data class Tun2socksTrafficStats(
    val txPackets: Long,
    val txBytes: Long,
    val rxPackets: Long,
    val rxBytes: Long,
)

class Tun2socksManager : ITun2socks {

    companion object {
        private const val TAG = "Tun2socksManager"

        /**
         * Generate YAML configuration file required by native hev-socks5-tunnel engine.
         */
        fun createYamlConfigFile(context: Context, config: ProxyConfig): File {
            val configFile = File(context.cacheDir, "socks5_tunnel.yml")
            val yamlContent = buildYamlConfig(config)
            configFile.writeText(yamlContent)
            AppLogger.d(TAG, "已生成 HEV 配置文件: ${configFile.absolutePath}（认证信息未写入日志）")
            return configFile
        }

        internal fun buildYamlConfig(config: ProxyConfig): String {
            return buildString {
                appendLine("tunnel:")
                appendLine("  name: tun0")
                appendLine("  mtu: ${config.mtu}")
                appendLine("  ipv4: ${TunnelAddresses.IPV4}")
                if (config.ipv6Mode == Ipv6Mode.PROXY) {
                    appendLine("  ipv6: '${TunnelAddresses.IPV6}'")
                }
                appendLine("socks5:")
                appendLine("  port: ${config.proxyPort}")
                appendLine("  address: ${yamlQuote(config.proxyHost.trim())}")
                appendLine("  udp: 'udp'")
                if (config.username.isNotEmpty()) {
                    appendLine("  username: ${yamlQuote(config.username)}")
                    appendLine("  password: ${yamlQuote(config.password)}")
                }
                appendLine("mapdns:")
                appendLine("  address: $MAP_DNS_ADDRESS")
                appendLine("  port: 53")
                appendLine("  network: $MAP_DNS_NETWORK")
                appendLine("  netmask: $MAP_DNS_NETMASK")
                appendLine("  cache-size: $MAP_DNS_CACHE_SIZE")
                appendLine("misc:")
                // HEV's client debug level prints the configured credentials.
                // Info retains connection and handshake details without that line.
                appendLine("  log-file: stderr")
                appendLine("  log-level: info")
            }
        }

        fun endpointDescription(config: ProxyConfig): String =
            "socks5://${config.proxyHost.trim()}:${config.proxyPort}"

        internal fun yamlQuote(value: String): String = "'${value.replace("'", "''")}'"

        /**
         * Upstream JNI order: tx packets, tx bytes, rx packets, rx bytes.
         * TX is read from TUN (upload); RX is written to TUN (download).
         */
        internal fun parseNativeStats(values: LongArray?): Tun2socksTrafficStats? {
            if (values == null || values.size < 4 || values.take(4).any { it < 0L }) {
                return null
            }
            return Tun2socksTrafficStats(
                txPackets = values[0],
                txBytes = values[1],
                rxPackets = values[2],
                rxBytes = values[3],
            )
        }
    }

    private var statsFailureLogged = false

    override fun start(context: Context, tunFd: Int, config: ProxyConfig): Boolean {
        AppLogger.setSensitiveValues(listOf(config.password))
        AppLogger.i(TAG, "启动 tun2socks 引擎 [TUN fd=$tunFd, ${endpointDescription(config)}]")
        if (!Tun2socksJni.isLibraryLoaded) {
            AppLogger.e(TAG, "未能加载原生动态库，拒绝建立虚假的已连接状态")
            return false
        }
        if (config.proxyHost.isBlank() || config.proxyPort !in 1..65535) {
            AppLogger.e(TAG, "SOCKS5 服务器地址或端口无效")
            return false
        }
        if (config.username.isEmpty() != config.password.isEmpty()) {
            AppLogger.e(TAG, "SOCKS5 用户名和密码必须同时填写或同时留空")
            return false
        }

        return try {
            val configFile = createYamlConfigFile(context, config)
            val started = Tun2socksJni.TProxyStartService(configFile.absolutePath, tunFd)
            if (started) {
                statsFailureLogged = false
                AppLogger.i(TAG, "原生 HEV 引擎启动调用已成功返回")
            } else {
                AppLogger.e(TAG, "原生 HEV 引擎拒绝启动")
            }
            started
        } catch (e: LinkageError) {
            AppLogger.e(TAG, "原生 HEV JNI 链接失败", e)
            false
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动原生 HEV 引擎出现异常", e)
            false
        }
    }

    override fun stop(): Boolean {
        AppLogger.i(TAG, "停止 tun2socks 原生引擎...")
        if (!Tun2socksJni.isLibraryLoaded) return true

        return try {
            val stopped = Tun2socksJni.TProxyStopService()
            if (stopped) {
                AppLogger.i(TAG, "原生 tun2socks 引擎已停止")
                statsFailureLogged = false
            } else {
                AppLogger.e(TAG, "原生 tun2socks 引擎停止调用失败")
            }
            stopped
        } catch (e: LinkageError) {
            AppLogger.e(TAG, "停止原生 HEV 引擎时 JNI 链接失败", e)
            false
        } catch (e: Exception) {
            AppLogger.e(TAG, "停止原生 tun2socks 引擎出现异常", e)
            false
        }
    }

    override fun isRunning(): Boolean {
        if (!Tun2socksJni.isLibraryLoaded) return false

        return try {
            Tun2socksJni.TProxyIsRunning()
        } catch (e: LinkageError) {
            logStatsFailureOnce("读取原生 HEV 运行状态时 JNI 链接失败", e)
            false
        } catch (e: Exception) {
            logStatsFailureOnce("读取原生 HEV 运行状态时出现异常", e)
            false
        }
    }

    override fun getTrafficStats(): Tun2socksTrafficStats? {
        if (!isRunning()) return null

        return try {
            val stats = parseNativeStats(Tun2socksJni.TProxyGetStats())
            if (stats == null) {
                logStatsFailureOnce("原生 HEV 返回了无效的流量统计数据")
            } else {
                statsFailureLogged = false
            }
            stats
        } catch (e: LinkageError) {
            logStatsFailureOnce("读取原生 HEV 流量统计时 JNI 链接失败", e)
            null
        } catch (e: Exception) {
            logStatsFailureOnce("读取原生 HEV 流量统计时出现异常", e)
            null
        }
    }

    private fun logStatsFailureOnce(message: String, error: Throwable? = null) {
        if (statsFailureLogged) return
        statsFailureLogged = true
        AppLogger.e(TAG, message, error)
    }
}
