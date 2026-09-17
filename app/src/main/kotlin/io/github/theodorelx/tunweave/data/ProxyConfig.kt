package io.github.theodorelx.tunweave.data

data class ProxyConfig(
    val proxyHost: String = "",
    val proxyPort: Int = 1080,
    val proxyType: ProxyType = ProxyType.SOCKS5,
    val username: String = "",
    val password: String = "",
    val ipv6Mode: Ipv6Mode = Ipv6Mode.PROXY,
    val bypassLan: Boolean = true,
    val bypassAddresses: String = DEFAULT_BYPASS_ADDRESSES,
    val mtu: Int = 1500,
    val autoReconnect: Boolean = true,
    // 测延迟配置
    val latencyTestUrl: String = "https://www.gstatic.com/generate_204",
    // 每应用代理
    val perAppMode: PerAppMode = PerAppMode.BLACKLIST,
    val selectedApps: Set<String> = emptySet(),
)

/**
 * Documentation-only benchmark range reserved for HEV MapDNS fake addresses.
 * It must be routed into the VPN so the native engine can restore the domain name.
 */
const val MAP_DNS_ADDRESS = "198.18.0.2"
const val MAP_DNS_NETWORK = "198.18.0.0"
const val MAP_DNS_NETMASK = "255.254.0.0"
const val MAP_DNS_CACHE_SIZE = 10_000

const val LEGACY_BYPASS_ADDRESSES =
    "10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8, 169.254.0.0/16"

const val DEFAULT_BYPASS_ADDRESSES =
    "$LEGACY_BYPASS_ADDRESSES, fc00::/7, fe80::/10, ::1/128"

enum class ProxyType(val displayName: String) {
    SOCKS5("SOCKS5")
}

enum class Ipv6Mode(val displayName: String) {
    PROXY("代理"),
    BLOCK("阻断"),
    BYPASS("直连"),
}

enum class PerAppMode(val displayName: String) {
    DISABLED("关闭（全局代理）"),
    WHITELIST("仅允许选中应用（白名单）"),
    BLACKLIST("绕过选中应用（黑名单）")
}
