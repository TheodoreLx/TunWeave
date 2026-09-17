package io.github.theodorelx.tunweave

import io.github.theodorelx.tunweave.data.DEFAULT_BYPASS_ADDRESSES
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.data.NetworkAddressParser
import io.github.theodorelx.tunweave.data.PerAppMode
import io.github.theodorelx.tunweave.data.ProxyConfig
import io.github.theodorelx.tunweave.data.ProxyType
import io.github.theodorelx.tunweave.data.exportAppSelection
import io.github.theodorelx.tunweave.data.importAppSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyConfigTest {

    @Test
    fun testProxyConfigDefaults() {
        val config = ProxyConfig()
        assertEquals("", config.proxyHost)
        assertEquals(1080, config.proxyPort)
        assertEquals(ProxyType.SOCKS5, config.proxyType)
        assertEquals(Ipv6Mode.PROXY, config.ipv6Mode)
        assertEquals(DEFAULT_BYPASS_ADDRESSES, config.bypassAddresses)
        assertTrue(config.bypassAddresses.contains("fc00::/7"))
        assertEquals("https://www.gstatic.com/generate_204", config.latencyTestUrl)
        assertTrue(config.bypassLan)
        assertEquals(1500, config.mtu)
        assertEquals(PerAppMode.BLACKLIST, config.perAppMode)
        assertTrue(config.selectedApps.isEmpty())
    }

    @Test
    fun testProxyConfigCopy() {
        val config = ProxyConfig(
            proxyHost = "127.0.0.1",
            proxyPort = 7890,
            proxyType = ProxyType.SOCKS5,
            latencyTestUrl = "https://cp.cloudflare.com/generate_204",
        )
        val copy = config.copy(proxyPort = 1080)
        assertEquals("127.0.0.1", copy.proxyHost)
        assertEquals(1080, copy.proxyPort)
        assertEquals(ProxyType.SOCKS5, copy.proxyType)
        assertEquals("https://cp.cloudflare.com/generate_204", copy.latencyTestUrl)
    }

    @Test
    fun appSelectionTransferUsesPortablePackageListAndRejectsInvalidEntries() {
        val exported = exportAppSelection(setOf("com.tencent.mm", "com.example.app"))
        assertTrue(exported.startsWith("# TunWeave app selection v1"))
        assertEquals(
            setOf("com.tencent.mm", "com.example.app"),
            importAppSelection("$exported\ninvalid package\ncom.example.app"),
        )
    }

    @Test
    fun networkCidrParserNormalizesIpv4AndIpv6() {
        val ipv4 = NetworkAddressParser.parseCidr("192.168.1.25/24")
        assertEquals("192.168.1.0", ipv4.networkAddress.hostAddress)
        assertEquals(24, ipv4.prefixLength)

        val ipv6 = NetworkAddressParser.parseCidr("fd12:3456:789a:1::1234/64")
        assertEquals(64, ipv6.prefixLength)
        assertTrue(ipv6.networkAddress.hostAddress.orEmpty().startsWith("fd12:3456:789a:1:"))
        assertTrue(ipv6.networkAddress.address.copyOfRange(8, 16).all { it == 0.toByte() })
    }

    @Test
    fun networkCidrParserRejectsHostnamesAndInvalidPrefixes() {
        assertThrowsIllegalArgument { NetworkAddressParser.parseCidr("example.com/24") }
        assertThrowsIllegalArgument { NetworkAddressParser.parseCidr("192.168.1.1/33") }
        assertThrowsIllegalArgument { NetworkAddressParser.parseCidr("fd00::1/129") }
        assertThrowsIllegalArgument { NetworkAddressParser.parseCidr("fe80::1%wlan0/64") }
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
