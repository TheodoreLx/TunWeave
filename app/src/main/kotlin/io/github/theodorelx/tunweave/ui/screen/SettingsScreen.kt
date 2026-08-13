package io.github.theodorelx.tunweave.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.data.PerAppMode
import io.github.theodorelx.tunweave.util.AppLogger
import io.github.theodorelx.tunweave.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAppSelect: () -> Unit,
    onNavigateToLogs: () -> Unit,
) {
    val config by viewModel.proxyConfig.collectAsStateWithLifecycle()
    val logs by AppLogger.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Latency Test Section
            SectionHeader("网络测试")

            OutlinedTextField(
                value = config.latencyTestUrl,
                onValueChange = { viewModel.updateLatencyTestUrl(it) },
                label = { Text("测延迟 URL") },
                placeholder = { Text("https://www.gstatic.com/generate_204") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Per-App Proxy Section
            SectionHeader("分流路由")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAppSelect() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "分应用代理",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = when (config.perAppMode) {
                                PerAppMode.DISABLED -> "关闭（全局代理）"
                                PerAppMode.WHITELIST -> "白名单模式（已选 ${config.selectedApps.size} 个应用）"
                                PerAppMode.BLACKLIST -> "黑名单模式（已选 ${config.selectedApps.size} 个应用）"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // DNS Section
            SectionHeader("DNS 配置")

            OutlinedTextField(
                value = config.dnsServer,
                onValueChange = { viewModel.updateDnsServer(it) },
                label = { Text("主 DNS 服务器") },
                placeholder = { Text("223.5.5.5") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = config.dnsServerAlt,
                onValueChange = { viewModel.updateDnsServerAlt(it) },
                label = { Text("备用 DNS 服务器") },
                placeholder = { Text("114.114.114.114") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("IPv6")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Ipv6Mode.entries.forEach { mode ->
                    FilterChip(
                        selected = config.ipv6Mode == mode,
                        onClick = { viewModel.updateIpv6Mode(mode) },
                        label = { Text(mode.displayName) },
                    )
                }
            }

            Text(
                text = when (config.ipv6Mode) {
                    Ipv6Mode.PROXY -> "IPv6 流量进入 VPN 并由 SOCKS5 代理（推荐）"
                    Ipv6Mode.BLOCK -> "阻断纳入 VPN 应用的 IPv6 流量"
                    Ipv6Mode.BYPASS -> "IPv6 通过底层网络直连，可能造成代理泄漏"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (config.ipv6Mode == Ipv6Mode.BYPASS) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Bypass Section
            SectionHeader("绕过地址")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "绕过局域网",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = config.bypassLan,
                    onCheckedChange = { viewModel.updateBypassLan(it) },
                )
            }

            OutlinedTextField(
                value = config.bypassAddresses,
                onValueChange = { viewModel.updateBypassAddresses(it) },
                label = { Text("自定义绕过地址") },
                placeholder = { Text("CIDR 格式，逗号分隔") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Debugging & Logs Section
            SectionHeader("调测与排查")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLogs() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "查看运行日志",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "实时记录 VPN 网卡、代理和连接测试日志 (${logs.size} 条)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Advanced Section
            SectionHeader("高级设置")

            OutlinedTextField(
                value = config.mtu.toString(),
                onValueChange = { viewModel.updateMtu(it) },
                label = { Text("MTU") },
                placeholder = { Text("1500") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "自动重连",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = config.autoReconnect,
                    onCheckedChange = { viewModel.updateAutoReconnect(it) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
