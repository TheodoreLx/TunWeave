package io.github.theodorelx.tunweave.ui.screen

import android.content.Intent
import android.net.VpnService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.theodorelx.tunweave.data.VpnState
import io.github.theodorelx.tunweave.service.ProxyVpnService
import io.github.theodorelx.tunweave.ui.component.ProxyConfigCard
import io.github.theodorelx.tunweave.ui.component.StatusCard
import io.github.theodorelx.tunweave.ui.component.TrafficStatsCard
import io.github.theodorelx.tunweave.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onRequestVpnPermission: (Intent) -> Unit,
) {
    val vpnState by ProxyVpnService.state.collectAsStateWithLifecycle()
    val trafficStats by ProxyVpnService.trafficStats.collectAsStateWithLifecycle()
    val config by viewModel.proxyConfig.collectAsStateWithLifecycle()
    val showAuth by viewModel.showAuthFields.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val isTestingLatency by viewModel.isTestingLatency.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TunWeave") },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.BugReport, contentDescription = "日志")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Connection status card
            StatusCard(
                vpnState = vpnState,
                connectedTimeSec = trafficStats.connectedTimeSec,
                isDark = isDark,
                latencyMs = latencyMs,
                isTestingLatency = isTestingLatency,
                onTestLatency = { viewModel.testLatency() },
                onToggle = {
                    when (vpnState) {
                        VpnState.CONNECTED, VpnState.CONNECTING -> {
                            viewModel.disconnectVpn(context)
                        }
                        VpnState.DISCONNECTED, VpnState.ERROR -> {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                onRequestVpnPermission(prepareIntent)
                            } else {
                                viewModel.connectVpn(context)
                            }
                        }
                        VpnState.DISCONNECTING -> { /* No-op while disconnecting */ }
                    }
                },
            )

            // Traffic stats (visible when connected)
            AnimatedVisibility(
                visible = vpnState == VpnState.CONNECTED,
                enter = slideInVertically { -it / 2 } + fadeIn(),
                exit = slideOutVertically { -it / 2 } + fadeOut(),
            ) {
                TrafficStatsCard(stats = trafficStats)
            }

            // Proxy config card
            ProxyConfigCard(
                config = config,
                isConnected = vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING,
                showAuth = showAuth,
                onHostChange = { viewModel.updateProxyHost(it) },
                onPortChange = { viewModel.updateProxyPort(it) },
                onUsernameChange = { viewModel.updateUsername(it) },
                onPasswordChange = { viewModel.updatePassword(it) },
                onToggleAuth = { viewModel.toggleAuthFields() },
            )

            // Bottom spacing
            Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
