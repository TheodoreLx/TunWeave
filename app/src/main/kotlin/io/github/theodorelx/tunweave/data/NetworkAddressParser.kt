package io.github.theodorelx.tunweave.data

import java.net.InetAddress

data class NetworkCidr(
    val networkAddress: InetAddress,
    val prefixLength: Int,
)

object NetworkAddressParser {

    fun parseNumericAddress(value: String): InetAddress {
        val address = value.trim()
        require(address.isNotEmpty()) { "IP 地址不能为空" }
        require('%' !in address) { "不支持带作用域的 IP 地址: $value" }
        require(
            if (':' in address) {
                address.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' || it == '.' }
            } else {
                address.all { it.isDigit() || it == '.' }
            }
        ) { "不是数字格式的 IP 地址: $value" }
        return InetAddress.getByName(address)
    }

    fun parseCidr(value: String): NetworkCidr {
        val parts = value.trim().split('/')
        require(parts.size == 2) { "CIDR 必须包含一个 /: $value" }

        val address = parseNumericAddress(parts[0])
        val maxPrefix = address.address.size * 8
        val prefix = parts[1].toIntOrNull()
            ?: throw IllegalArgumentException("CIDR 前缀不是数字: $value")
        require(prefix in 0..maxPrefix) { "CIDR 前缀必须在 0..$maxPrefix: $value" }

        return NetworkCidr(
            networkAddress = normalizeNetworkAddress(address, prefix),
            prefixLength = prefix,
        )
    }

    private fun normalizeNetworkAddress(address: InetAddress, prefix: Int): InetAddress {
        val bytes = address.address
        var remainingPrefix = prefix

        for (index in bytes.indices) {
            if (remainingPrefix >= 8) {
                remainingPrefix -= 8
            } else if (remainingPrefix > 0) {
                val mask = (0xFF shl (8 - remainingPrefix)) and 0xFF
                bytes[index] = (bytes[index].toInt() and mask).toByte()
                remainingPrefix = 0
            } else {
                bytes[index] = 0
            }
        }
        return InetAddress.getByAddress(bytes)
    }
}
