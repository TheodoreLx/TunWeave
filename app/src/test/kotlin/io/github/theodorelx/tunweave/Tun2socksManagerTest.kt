package io.github.theodorelx.tunweave

import io.github.theodorelx.tunweave.data.ProxyConfig
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.tun2socks.Tun2socksManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Tun2socksManagerTest {

    @Test
    fun endpointDescriptionUsesSocks5WithoutCredentials() {
        val config = ProxyConfig(
            proxyHost = "192.168.1.100",
            proxyPort = 1080,
            username = "user",
            password = "pass",
        )

        assertEquals(
            "socks5://192.168.1.100:1080",
            Tun2socksManager.endpointDescription(config),
        )
    }

    @Test
    fun yamlQuoteEscapesSingleQuotes() {
        assertEquals("'user''name'", Tun2socksManager.yamlQuote("user'name"))
    }

    @Test
    fun nativeTrafficStatsUseUpstreamFieldOrder() {
        val stats = Tun2socksManager.parseNativeStats(longArrayOf(3L, 1_024L, 5L, 4_096L))!!
        assertEquals(3L, stats.txPackets)
        assertEquals(1_024L, stats.txBytes)
        assertEquals(5L, stats.rxPackets)
        assertEquals(4_096L, stats.rxBytes)
    }

    @Test
    fun invalidNativeTrafficStatsAreRejected() {
        assertNull(Tun2socksManager.parseNativeStats(null))
        assertNull(Tun2socksManager.parseNativeStats(longArrayOf(1L, 2L, 3L)))
        assertNull(Tun2socksManager.parseNativeStats(longArrayOf(1L, -2L, 3L, 4L)))
    }

    @Test
    fun hevYamlEnablesIpv6ByDefault() {
        val yaml = Tun2socksManager.buildYamlConfig(ProxyConfig())
        assertTrue(yaml.contains("  ipv4: 10.0.0.2"))
        assertTrue(yaml.contains("  ipv6: 'fd00:1::2'"))
    }

    @Test
    fun hevYamlOmitsIpv6WhenBlockedOrBypassed() {
        val blocked = Tun2socksManager.buildYamlConfig(
            ProxyConfig(ipv6Mode = Ipv6Mode.BLOCK),
        )
        val bypassed = Tun2socksManager.buildYamlConfig(
            ProxyConfig(ipv6Mode = Ipv6Mode.BYPASS),
        )
        assertFalse(blocked.contains("ipv6:"))
        assertFalse(bypassed.contains("ipv6:"))
    }

    @Test
    fun hevYamlUsesCredentialSafeNativeLogLevel() {
        val yaml = Tun2socksManager.buildYamlConfig(ProxyConfig())
        assertTrue(yaml.contains("  log-file: stderr"))
        assertTrue(yaml.contains("  log-level: info"))
        assertFalse(yaml.contains("log-level: debug"))
    }
}
