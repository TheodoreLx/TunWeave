package io.github.theodorelx.tunweave.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.theodorelx.tunweave.TunWeaveApp
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.data.PerAppMode
import io.github.theodorelx.tunweave.data.ProxyConfig
import io.github.theodorelx.tunweave.service.ProxyVpnService
import io.github.theodorelx.tunweave.util.LatencyTester
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TunWeaveApp).repository

    private val _proxyConfig = MutableStateFlow(ProxyConfig())
    val proxyConfig: StateFlow<ProxyConfig> = _proxyConfig.asStateFlow()

    private val _showAuthFields = MutableStateFlow(false)
    val showAuthFields: StateFlow<Boolean> = _showAuthFields.asStateFlow()

    // 测延迟状态
    private val _latencyMs = MutableStateFlow<Long?>(null) // null: 未测试, -1: 失败, >0: 毫秒
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    private val _isTestingLatency = MutableStateFlow(false)
    val isTestingLatency: StateFlow<Boolean> = _isTestingLatency.asStateFlow()

    private var saveJob: Job? = null
    private var latencyJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = repository.configFlow.first()
            _proxyConfig.value = saved
            if (saved.username.isNotEmpty()) {
                _showAuthFields.value = true
            }
        }
    }

    // ---- Latency test ----

    fun testLatency() {
        if (_isTestingLatency.value) return
        latencyJob?.cancel()
        latencyJob = viewModelScope.launch {
            _isTestingLatency.value = true
            _latencyMs.value = null
            val result = LatencyTester.testProxyLatency(_proxyConfig.value)
            _latencyMs.value = result
            _isTestingLatency.value = false
        }
    }

    // ---- Config update functions ----

    fun updateProxyHost(host: String) {
        _proxyConfig.value = _proxyConfig.value.copy(proxyHost = host)
        _latencyMs.value = null
        debounceSave()
    }

    fun updateProxyPort(port: String) {
        val portInt = port.toIntOrNull() ?: return
        if (portInt in 1..65535) {
            _proxyConfig.value = _proxyConfig.value.copy(proxyPort = portInt)
            _latencyMs.value = null
            debounceSave()
        }
    }

    fun updateUsername(username: String) {
        _proxyConfig.value = _proxyConfig.value.copy(username = username)
        debounceSave()
    }

    fun updatePassword(password: String) {
        _proxyConfig.value = _proxyConfig.value.copy(password = password)
        debounceSave()
    }

    fun updateDnsServer(dns: String) {
        _proxyConfig.value = _proxyConfig.value.copy(dnsServer = dns)
        debounceSave()
    }

    fun updateDnsServerAlt(dns: String) {
        _proxyConfig.value = _proxyConfig.value.copy(dnsServerAlt = dns)
        debounceSave()
    }

    fun updateIpv6Mode(mode: Ipv6Mode) {
        _proxyConfig.value = _proxyConfig.value.copy(ipv6Mode = mode)
        debounceSave()
    }

    fun updateBypassLan(enabled: Boolean) {
        _proxyConfig.value = _proxyConfig.value.copy(bypassLan = enabled)
        debounceSave()
    }

    fun updateBypassAddresses(addresses: String) {
        _proxyConfig.value = _proxyConfig.value.copy(bypassAddresses = addresses)
        debounceSave()
    }

    fun updateMtu(mtu: String) {
        val mtuInt = mtu.toIntOrNull() ?: return
        if (mtuInt in 1280..9000) {
            _proxyConfig.value = _proxyConfig.value.copy(mtu = mtuInt)
            debounceSave()
        }
    }

    fun updateAutoReconnect(enabled: Boolean) {
        _proxyConfig.value = _proxyConfig.value.copy(autoReconnect = enabled)
        debounceSave()
    }

    fun updateLatencyTestUrl(url: String) {
        _proxyConfig.value = _proxyConfig.value.copy(latencyTestUrl = url)
        _latencyMs.value = null
        debounceSave()
    }

    fun updatePerAppMode(mode: PerAppMode) {
        _proxyConfig.value = _proxyConfig.value.copy(perAppMode = mode)
        debounceSave()
    }

    fun toggleAppSelected(packageName: String) {
        val current = _proxyConfig.value.selectedApps
        val updated = if (current.contains(packageName)) {
            current - packageName
        } else {
            current + packageName
        }
        _proxyConfig.value = _proxyConfig.value.copy(selectedApps = updated)
        debounceSave()
    }

    fun updateSelectedApps(apps: Set<String>) {
        _proxyConfig.value = _proxyConfig.value.copy(selectedApps = apps)
        debounceSave()
    }

    fun toggleAuthFields() {
        _showAuthFields.value = !_showAuthFields.value
    }

    // ---- VPN control ----

    fun connectVpn(context: Context) {
        val config = _proxyConfig.value
        saveJob?.cancel()
        viewModelScope.launch {
            repository.saveConfig(config)
            ContextCompat.startForegroundService(context, ProxyVpnService.buildStartIntent(context))
        }
    }

    fun disconnectVpn(context: Context) {
        val intent = ProxyVpnService.buildStopIntent(context)
        context.startService(intent)
    }

    // ---- Auto-save with debounce ----

    private fun debounceSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            repository.saveConfig(_proxyConfig.value)
        }
    }
}
