package io.github.theodorelx.tunweave.ui.tv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.theodorelx.tunweave.data.AppInfo
import io.github.theodorelx.tunweave.data.AppListRepository
import io.github.theodorelx.tunweave.data.exportAppSelection
import io.github.theodorelx.tunweave.data.importAppSelection
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.data.PerAppMode
import io.github.theodorelx.tunweave.data.VpnState
import io.github.theodorelx.tunweave.service.ProxyVpnService
import io.github.theodorelx.tunweave.ui.component.ProxyConfigCard
import io.github.theodorelx.tunweave.ui.component.StatusCard
import io.github.theodorelx.tunweave.ui.component.TrafficStatsCard
import io.github.theodorelx.tunweave.ui.component.tvClickableItem
import io.github.theodorelx.tunweave.ui.component.tvFocusHighlight
import io.github.theodorelx.tunweave.ui.component.ReleaseUiDataWhenBackgrounded
import io.github.theodorelx.tunweave.util.AppLogger
import io.github.theodorelx.tunweave.util.LogLevel
import io.github.theodorelx.tunweave.viewmodel.MainViewModel

private enum class TvNavDestination(val title: String, val icon: ImageVector) {
    HOME("连接概览", Icons.Default.Home),
    APPS("分应用代理", Icons.Default.Apps),
    SETTINGS("系统设置", Icons.Default.Settings),
}

@Composable
fun TvMainScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: (Intent) -> Unit,
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Navigation Rail for TV
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            header = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TunWeave",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "TV 版",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        ) {
            TvNavDestination.entries.forEachIndexed { index, destination ->
                NavigationRailItem(
                    selected = selectedNavIndex == index,
                    onClick = { selectedNavIndex = index },
                    icon = { Icon(destination.icon, contentDescription = destination.title) },
                    label = { Text(destination.title, fontSize = 12.sp) },
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .tvFocusHighlight(
                            shape = RoundedCornerShape(12.dp),
                            focusedBorderWidth = 2.dp,
                            focusedScale = 1.05f,
                        ),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        // Right Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            when (TvNavDestination.entries[selectedNavIndex]) {
                TvNavDestination.HOME -> TvHomePane(
                    viewModel = viewModel,
                    onRequestVpnPermission = onRequestVpnPermission,
                )
                TvNavDestination.APPS -> TvAppSelectPane(viewModel = viewModel)
                TvNavDestination.SETTINGS -> TvSettingsPane(viewModel = viewModel)
            }
        }
    }
}

/**
 * TV Home Pane: Wide Dual-Column layout (Left: Status & Stats, Right: Proxy Config)
 */
@Composable
private fun TvHomePane(
    viewModel: MainViewModel,
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Left Column: Status Card + Traffic Stats
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                        VpnState.DISCONNECTING -> {}
                    }
                },
            )

            AnimatedVisibility(
                visible = vpnState == VpnState.CONNECTED,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TrafficStatsCard(stats = trafficStats)
            }
        }

        // Right Column: Proxy Configuration
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
        }
    }
}

/**
 * TV App Select Pane: Mode selector + Filtered App List with TV focus highlights
 */
@Composable
private fun TvAppSelectPane(viewModel: MainViewModel) {
    val context = LocalContext.current
    val appListRepository = remember(context) { AppListRepository(context) }
    val config by viewModel.proxyConfig.collectAsStateWithLifecycle()

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterSystemApps by remember { mutableStateOf(false) }
    var reloadGeneration by remember { mutableStateOf(0) }
    var isUiVisible by remember { mutableStateOf(true) }

    LaunchedEffect(reloadGeneration, isUiVisible) {
        if (!isUiVisible) return@LaunchedEffect
        installedApps = appListRepository.getInstalledApps()
        isLoading = false
    }

    ReleaseUiDataWhenBackgrounded(
        onRelease = {
            isUiVisible = false
            installedApps = emptyList()
            searchQuery = ""
            filterSystemApps = false
            appListRepository.clearIconCache()
        },
        onResume = {
            isUiVisible = true
            isLoading = true
            reloadGeneration += 1
        },
    )

    val filteredApps = remember(installedApps, searchQuery, filterSystemApps) {
        installedApps.filter { app ->
            (filterSystemApps || !app.isSystemApp) &&
            (searchQuery.isEmpty() ||
             app.appName.contains(searchQuery, ignoreCase = true) ||
             app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "分应用分流模式（已选 ${config.selectedApps.size} 个应用）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            SingleChoiceSegmentedButtonRow {
                PerAppMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = config.perAppMode == mode,
                        onClick = { viewModel.updatePerAppMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PerAppMode.entries.size),
                        modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                    ) {
                        Text(
                            text = when (mode) {
                                PerAppMode.DISABLED -> "全局"
                                PerAppMode.WHITELIST -> "白名单"
                                PerAppMode.BLACKLIST -> "黑名单"
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (config.perAppMode != PerAppMode.DISABLED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索应用名称或包名…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusHighlight(focusedScale = 1.01f),
                )

                FilterChip(
                    selected = filterSystemApps,
                    onClick = { filterSystemApps = !filterSystemApps },
                    label = { Text("包含系统应用") },
                    modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                )

                TextButton(
                    onClick = {
                        val allPkgs = filteredApps.map { it.packageName }.toSet()
                        viewModel.updateSelectedApps(config.selectedApps + allPkgs)
                    },
                    modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                ) {
                    Text("全选")
                }

                TextButton(
                    onClick = {
                        val currentPkgs = filteredApps.map { it.packageName }.toSet()
                        viewModel.updateSelectedApps(config.selectedApps - currentPkgs)
                    },
                    modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                ) {
                    Text("取消全选")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "TunWeave app selection",
                                exportAppSelection(config.selectedApps),
                            ),
                        )
                        Toast.makeText(context, "已导出 ${config.selectedApps.size} 个应用到剪贴板", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                ) {
                    Text("导出名单")
                }
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val content = clipboard.primaryClip?.getItemAt(0)
                            ?.coerceToText(context)?.toString().orEmpty()
                        val imported = importAppSelection(content)
                        val installedPackages = installedApps.mapTo(mutableSetOf<String>()) { it.packageName }
                        val accepted = imported.intersect(installedPackages)
                        viewModel.updateSelectedApps(accepted)
                        val ignored = imported.size - accepted.size
                        Toast.makeText(
                            context,
                            "已导入 ${accepted.size} 个应用" + if (ignored > 0) "，忽略 $ignored 个未安装应用" else "",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                ) {
                    Text("从剪贴板导入")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isChecked = config.selectedApps.contains(app.packageName)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvClickableItem(
                                    onClick = { viewModel.toggleAppSelected(app.packageName) },
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.02f,
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val iconBitmap by produceState<android.graphics.Bitmap?>(null, app.packageName) {
                                    value = appListRepository.getAppIcon(app.packageName)
                                }
                                val imageBitmap = remember(iconBitmap) { iconBitmap?.asImageBitmap() }

                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = app.appName,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleAppSelected(app.packageName) },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "当前处于「全局代理」模式，电视上所有应用均走代理。\n如需按应用分流，请在上方切换为「白名单」或「黑名单」模式。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * TV Settings Pane: Multi-Section 2-Column layout
 */
@Composable
private fun TvSettingsPane(viewModel: MainViewModel) {
    val config by viewModel.proxyConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportLogsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(AppLogger.exportText())
                } ?: error("无法打开目标文件")
                Toast.makeText(context, "日志已导出", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "日志导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Left Column: Network Test and IPv6
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "网络测试",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    OutlinedTextField(
                        value = config.latencyTestUrl,
                        onValueChange = { viewModel.updateLatencyTestUrl(it) },
                        label = { Text("测延迟 URL") },
                        placeholder = { Text("https://www.gstatic.com/generate_204") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(focusedScale = 1.01f),
                    )

                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "IPv6 模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Ipv6Mode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.ipv6Mode == mode,
                                onClick = { viewModel.updateIpv6Mode(mode) },
                                label = { Text(mode.displayName) },
                                modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
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
                        color = if (config.ipv6Mode == Ipv6Mode.BYPASS) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Right Column: Bypass & Advanced Settings
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "绕过规则",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("绕过局域网", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = config.bypassLan,
                            onCheckedChange = { viewModel.updateBypassLan(it) },
                            modifier = Modifier.tvFocusHighlight(focusedScale = 1.1f),
                        )
                    }

                    OutlinedTextField(
                        value = config.bypassAddresses,
                        onValueChange = { viewModel.updateBypassAddresses(it) },
                        label = { Text("自定义绕过地址 (CIDR)") },
                        placeholder = { Text("例如: 10.0.0.0/8, 192.168.0.0/16") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(focusedScale = 1.01f),
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "网络与高级选项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    OutlinedTextField(
                        value = config.mtu.toString(),
                        onValueChange = { viewModel.updateMtu(it) },
                        label = { Text("MTU") },
                        placeholder = { Text("1500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(focusedScale = 1.01f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("自动重连", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = config.autoReconnect,
                            onCheckedChange = { viewModel.updateAutoReconnect(it) },
                            modifier = Modifier.tvFocusHighlight(focusedScale = 1.1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用日志", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "默认关闭；导出时会隐藏密码和密钥。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = config.loggingEnabled,
                            onCheckedChange = { viewModel.updateLoggingEnabled(it) },
                            modifier = Modifier.tvFocusHighlight(focusedScale = 1.1f),
                        )
                    }

                    Text("最低日志级别", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogLevel.entries.forEach { level ->
                            FilterChip(
                                selected = config.logLevel == level,
                                onClick = { viewModel.updateLogLevel(level) },
                                enabled = config.loggingEnabled,
                                label = { Text(level.displayName) },
                                modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            val text = AppLogger.exportText()
                            if (text.isEmpty()) {
                                Toast.makeText(context, "暂无可导出的日志", Toast.LENGTH_SHORT).show()
                            } else {
                                exportLogsLauncher.launch("TunWeave-logs.txt")
                            }
                        },
                        modifier = Modifier.tvFocusHighlight(focusedScale = 1.05f),
                    ) {
                        Text("导出日志文件")
                    }
                }
            }
        }
    }
}
