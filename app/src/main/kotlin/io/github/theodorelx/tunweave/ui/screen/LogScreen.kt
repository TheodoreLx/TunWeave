package io.github.theodorelx.tunweave.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.theodorelx.tunweave.util.AppLogger
import io.github.theodorelx.tunweave.util.LogEntry
import io.github.theodorelx.tunweave.util.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logs by AppLogger.logs.collectAsStateWithLifecycle()
    var selectedLevelFilter by remember { mutableStateOf<LogLevel?>(null) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedLevelFilter) {
        if (selectedLevelFilter == null) logs
        else logs.filter { it.level == selectedLevelFilter }
    }

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用日志与调测") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = logs.joinToString("\n") { "[${it.timestamp}][${it.level}][${it.tag}] ${it.message}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("TunWeave Logs", text))
                        Toast.makeText(context, "已复制所有日志到剪贴板", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制日志")
                    }
                    IconButton(onClick = { AppLogger.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清空日志")
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
            // Level Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedLevelFilter == null,
                    onClick = { selectedLevelFilter = null },
                    label = { Text("全部 (${logs.size})") },
                )
                LogLevel.entries.forEach { level ->
                    val count = logs.count { it.level == level }
                    FilterChip(
                        selected = selectedLevelFilter == level,
                        onClick = {
                            selectedLevelFilter = if (selectedLevelFilter == level) null else level
                        },
                        label = { Text("${level.name} ($count)") },
                    )
                }
            }

            HorizontalDivider()

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无相关日志记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredLogs) { entry ->
                        LogItemRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                LevelBadge(level = entry.level)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                ),
                color = when (entry.level) {
                    LogLevel.ERROR -> Color(0xFFD32F2F)
                    LogLevel.WARN -> Color(0xFFF57F17)
                    LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                    LogLevel.DEBUG -> Color(0xFF757575)
                },
            )
        }
    }
}

@Composable
private fun LevelBadge(level: LogLevel) {
    val (bg, fg) = when (level) {
        LogLevel.DEBUG -> Color(0xFFE0E0E0) to Color(0xFF424242)
        LogLevel.INFO -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        LogLevel.WARN -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        LogLevel.ERROR -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = bg,
    ) {
        Text(
            text = level.name,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
            color = fg,
        )
    }
}
