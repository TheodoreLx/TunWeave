package io.github.theodorelx.tunweave.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.theodorelx.tunweave.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proxy_config")

class ProxyRepository(private val context: Context) {

    private val credentialCipher = CredentialCipher()

    private companion object {
        private const val TAG = "ProxyRepository"
    }

    private object Keys {
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val PROXY_TYPE = stringPreferencesKey("proxy_type")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val DNS_SERVER = stringPreferencesKey("dns_server")
        val DNS_SERVER_ALT = stringPreferencesKey("dns_server_alt")
        val IPV6_MODE = stringPreferencesKey("ipv6_mode")
        val BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val BYPASS_ADDRESSES = stringPreferencesKey("bypass_addresses")
        val MTU = intPreferencesKey("mtu")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val LATENCY_TEST_URL = stringPreferencesKey("latency_test_url")
        val PER_APP_MODE = stringPreferencesKey("per_app_mode")
        val SELECTED_APPS = stringSetPreferencesKey("selected_apps")
    }

    val configFlow: Flow<ProxyConfig> = context.dataStore.data.map { prefs ->
        val defaults = ProxyConfig()
        val storedIpv6Mode = prefs[Keys.IPV6_MODE]
        val storedBypassAddresses = prefs[Keys.BYPASS_ADDRESSES]
        val config = ProxyConfig(
            proxyHost = prefs[Keys.PROXY_HOST] ?: defaults.proxyHost,
            proxyPort = prefs[Keys.PROXY_PORT] ?: defaults.proxyPort,
            proxyType = prefs[Keys.PROXY_TYPE]?.let {
                try { ProxyType.valueOf(it) } catch (_: Exception) { defaults.proxyType }
            } ?: defaults.proxyType,
            username = prefs[Keys.USERNAME] ?: defaults.username,
            password = decryptPassword(prefs[Keys.PASSWORD] ?: defaults.password),
            dnsServer = prefs[Keys.DNS_SERVER] ?: defaults.dnsServer,
            dnsServerAlt = prefs[Keys.DNS_SERVER_ALT] ?: defaults.dnsServerAlt,
            ipv6Mode = storedIpv6Mode?.let {
                try { Ipv6Mode.valueOf(it) } catch (_: Exception) { defaults.ipv6Mode }
            } ?: defaults.ipv6Mode,
            bypassLan = prefs[Keys.BYPASS_LAN] ?: defaults.bypassLan,
            bypassAddresses = when {
                storedBypassAddresses == null -> defaults.bypassAddresses
                storedIpv6Mode == null && storedBypassAddresses == LEGACY_BYPASS_ADDRESSES ->
                    defaults.bypassAddresses
                else -> storedBypassAddresses
            },
            mtu = prefs[Keys.MTU] ?: defaults.mtu,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: defaults.autoReconnect,
            latencyTestUrl = prefs[Keys.LATENCY_TEST_URL] ?: defaults.latencyTestUrl,
            perAppMode = prefs[Keys.PER_APP_MODE]?.let {
                try { PerAppMode.valueOf(it) } catch (_: Exception) { defaults.perAppMode }
            } ?: defaults.perAppMode,
            selectedApps = prefs[Keys.SELECTED_APPS] ?: defaults.selectedApps,
        )
        AppLogger.setSensitiveValues(listOf(config.password))
        AppLogger.d(TAG, "从 DataStore 读取代理配置: ${config.proxyType}://${config.proxyHost}:${config.proxyPort}")
        config
    }

    suspend fun saveConfig(config: ProxyConfig) {
        AppLogger.setSensitiveValues(listOf(config.password))
        context.dataStore.edit { prefs ->
            prefs[Keys.PROXY_HOST] = config.proxyHost
            prefs[Keys.PROXY_PORT] = config.proxyPort
            prefs[Keys.PROXY_TYPE] = config.proxyType.name
            prefs[Keys.USERNAME] = config.username
            prefs[Keys.PASSWORD] = credentialCipher.encrypt(config.password)
            prefs[Keys.DNS_SERVER] = config.dnsServer
            prefs[Keys.DNS_SERVER_ALT] = config.dnsServerAlt
            prefs[Keys.IPV6_MODE] = config.ipv6Mode.name
            prefs[Keys.BYPASS_LAN] = config.bypassLan
            prefs[Keys.BYPASS_ADDRESSES] = config.bypassAddresses
            prefs[Keys.MTU] = config.mtu
            prefs[Keys.AUTO_RECONNECT] = config.autoReconnect
            prefs[Keys.LATENCY_TEST_URL] = config.latencyTestUrl
            prefs[Keys.PER_APP_MODE] = config.perAppMode.name
            prefs[Keys.SELECTED_APPS] = config.selectedApps
        }
        AppLogger.i(TAG, "代理配置已保存到 DataStore (${config.proxyType}://${config.proxyHost}:${config.proxyPort})")
    }

    private fun decryptPassword(stored: String): String = try {
        credentialCipher.decrypt(stored)
    } catch (e: Exception) {
        AppLogger.e(TAG, "无法解密已保存的代理密码，已按空密码处理", e)
        ""
    }
}
