package io.github.theodorelx.tunweave

import io.github.theodorelx.tunweave.data.ProxyConfig
import io.github.theodorelx.tunweave.util.LatencyTester
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

class LatencyTesterTest {

    @Test
    fun testLatencyAgainstFakeSocks5Server() = runBlocking {
        ServerSocket(0).use { server ->
            val worker = thread {
                server.accept().use { socket ->
                    val input = DataInputStream(socket.getInputStream())
                    val output = DataOutputStream(socket.getOutputStream())
                    assertTrue(input.readUnsignedByte() == 5)
                    val methods = ByteArray(input.readUnsignedByte())
                    input.readFully(methods)
                    output.write(byteArrayOf(5, 0))
                    output.flush()

                    assertTrue(input.readUnsignedByte() == 5)
                    input.readUnsignedByte()
                    input.readUnsignedByte()
                    assertTrue(input.readUnsignedByte() == 3)
                    val domain = ByteArray(input.readUnsignedByte())
                    input.readFully(domain)
                    input.readUnsignedShort()
                    output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 80))
                    output.flush()
                }
            }

            val latency = LatencyTester.testProxyLatency(
                ProxyConfig(proxyHost = "127.0.0.1", proxyPort = server.localPort)
            )
            worker.join()
            assertTrue("Latency should be greater than 0 ms", latency > 0)
        }
    }

    @Test
    fun testLatencyInvalidProxyReturnsNegativeOne() = runBlocking {
        val closedPort = ServerSocket(0).use { it.localPort }
        val config = ProxyConfig(proxyHost = "127.0.0.1", proxyPort = closedPort)
        val latency = LatencyTester.testProxyLatency(config)
        assertTrue("Invalid proxy should return -1", latency == -1L)
    }
}
