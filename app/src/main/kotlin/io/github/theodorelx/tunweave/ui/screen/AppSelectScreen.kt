package io.github.theodorelx.tunweave.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.theodorelx.tunweave.data.AppInfo
import io.github.theodorelx.tunweave.data.AppListRepository
import io.github.theodorelx.tunweave.data.PerAppMode
import io.github.theodorelx.tunweave.data.exportAppSelection
import io.github.theodorelx.tunweave.data.importAppSelection
import io.github.theodorelx.tunweave.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val config by viewModel.proxyConfig.collectAsStateWithLifecycle()

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val repo = AppListRepository(context)
        installedApps = repo.getInstalledApps()
        isLoading = false
    }

    val filteredApps = remember(installedApps, searchQuery, filterSystemApps) {
        installedApps.filter { app ->
            (filterSystemApps || !app.isSystemApp) &&
            (searchQuery.isEmpty() ||
             app.appName.contains(searchQuery, ignoreCase = true) ||
             app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("分应用代理设置")
                        Text(
                            text = "已选 ${config.selectedApps.size} 个应用",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
                .padding(padding),
        ) {
            // Mode selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "代理模式",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PerAppMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = config.perAppMode == mode,
                            onClick = { viewModel.updatePerAppMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = PerAppMode.entries.size),
                        ) {
                            Text(
                                text = when (mode) {
                                    PerAppMode.DISABLED -> "全局"
                                    PerAppMode.WHITELIST -> "白名单"
                                    PerAppMode.BLACKLIST -> "黑名单"
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            if (config.perAppMode != PerAppMode.DISABLED) {
                // Search & Filter header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = filterSystemApps,
                            onClick = { filterSystemApps = !filterSystemApps },
                            label = { Text("包含系统应用") },
                        )

                        Row {
                            TextButton(onClick = {
                                val allPkgs = filteredApps.map { it.packageName }.toSet()
                                viewModel.updateSelectedApps(config.selectedApps + allPkgs)
                            }) {
                                Text("全选")
                            }
                            TextButton(onClick = {
                                val currentPkgs = filteredApps.map { it.packageName }.toSet()
                                viewModel.updateSelectedApps(config.selectedApps - currentPkgs)
                            }) {
                                Text("取消全选")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "TunWeave app selection",
                                    exportAppSelection(config.selectedApps),
                                ),
                            )
                            Toast.makeText(context, "已导出 ${config.selectedApps.size} 个应用到剪贴板", Toast.LENGTH_SHORT).show()
                        }) {
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
                        ) {
                            Text("从剪贴板导入")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // App List
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("正在加载已安装应用…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppItemRow(
                                app = app,
                                isChecked = config.selectedApps.contains(app.packageName),
                                onToggle = {
                                    viewModel.toggleAppSelected(app.packageName)
                                },
                            )
                        }
                    }
                }
            } else {
                // Disabled mode message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "当前已开启「全局代理」模式，所有应用的流量均会通过代理转发。\n\n如需指定部分应用走代理或绕过代理，请在上方切换为「白名单」或「黑名单」模式。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItemRow(
    app: AppInfo,
    isChecked: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            val imageBitmap = remember(app.icon) { app.icon?.toImageBitmap() }
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "系统",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap? {
    try {
        if (this is BitmapDrawable && this.bitmap != null) {
            return this.bitmap.asImageBitmap()
        }
        val width = if (intrinsicWidth > 0) intrinsicWidth else 96
        val height = if (intrinsicHeight > 0) intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap.asImageBitmap()
    } catch (_: Exception) {
        return null
    }
}
