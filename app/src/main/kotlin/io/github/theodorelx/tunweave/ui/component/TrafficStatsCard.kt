package io.github.theodorelx.tunweave.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.theodorelx.tunweave.data.TrafficStats
import io.github.theodorelx.tunweave.ui.theme.DownloadColor
import io.github.theodorelx.tunweave.ui.theme.DownloadColorDark
import io.github.theodorelx.tunweave.ui.theme.MemoryColor
import io.github.theodorelx.tunweave.ui.theme.UploadColor
import io.github.theodorelx.tunweave.ui.theme.UploadColorDark

@Composable
fun TrafficStatsCard(
    stats: TrafficStats,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val uploadTint = if (isDark) UploadColorDark else UploadColor
    val downloadTint = if (isDark) DownloadColorDark else DownloadColor

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "流量统计",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Speed row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Upload speed
                StatItem(
                    icon = Icons.Default.ArrowUpward,
                    tint = uploadTint,
                    label = "上传速度",
                    value = formatSpeed(stats.uploadSpeed),
                )
                // Download speed
                StatItem(
                    icon = Icons.Default.ArrowDownward,
                    tint = downloadTint,
                    label = "下载速度",
                    value = formatSpeed(stats.downloadSpeed),
                )
            }

            // Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Total upload
                StatItem(
                    icon = Icons.Default.ArrowUpward,
                    tint = uploadTint,
                    label = "总上传",
                    value = formatBytes(stats.totalUpload),
                )
                // Total download
                StatItem(
                    icon = Icons.Default.ArrowDownward,
                    tint = downloadTint,
                    label = "总下载",
                    value = formatBytes(stats.totalDownload),
                )
            }

            // Process proportional set size (PSS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MemoryColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "内存（PSS）",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "总计 %.1f MB".format(stats.memory.totalPssMb),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }

            MemoryStatsRow(
                items = listOf(
                    "Java" to stats.memory.javaPssMb,
                    "Native" to stats.memory.nativePssMb,
                    "代码" to stats.memory.codePssMb,
                    "图形" to stats.memory.graphicsPssMb,
                ),
            )
            MemoryStatsRow(
                items = listOf(
                    "栈" to stats.memory.stackPssMb,
                    "私有其他" to stats.memory.privateOtherPssMb,
                    "系统/共享" to stats.memory.systemPssMb,
                ),
            )
        }
    }
}

@Composable
private fun MemoryStatsRow(items: List<Pair<String, Float>>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        items.forEach { (label, value) ->
            MemoryStatItem(
                label = label,
                value = value,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MemoryStatItem(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "%.1f MB".format(value),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = tint,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatSpeed(bytesPerSec: Long): String = when {
    bytesPerSec < 1024 -> "$bytesPerSec B/s"
    bytesPerSec < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSec / 1024.0)
    bytesPerSec < 1024L * 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024))
    else -> "%.2f GB/s".format(bytesPerSec / (1024.0 * 1024 * 1024))
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
