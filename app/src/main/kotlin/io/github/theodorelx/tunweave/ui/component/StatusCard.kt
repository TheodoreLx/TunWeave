package io.github.theodorelx.tunweave.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.theodorelx.tunweave.data.VpnState
import io.github.theodorelx.tunweave.ui.theme.VpnConnectedContainerDark
import io.github.theodorelx.tunweave.ui.theme.VpnConnectedContainerLight
import io.github.theodorelx.tunweave.ui.theme.VpnConnectedDark
import io.github.theodorelx.tunweave.ui.theme.VpnConnectedLight
import io.github.theodorelx.tunweave.ui.theme.VpnConnectingContainerDark
import io.github.theodorelx.tunweave.ui.theme.VpnConnectingContainerLight
import io.github.theodorelx.tunweave.ui.theme.VpnConnectingDark
import io.github.theodorelx.tunweave.ui.theme.VpnConnectingLight
import io.github.theodorelx.tunweave.ui.theme.VpnErrorContainerDark
import io.github.theodorelx.tunweave.ui.theme.VpnErrorContainerLight
import io.github.theodorelx.tunweave.ui.theme.VpnErrorDark
import io.github.theodorelx.tunweave.ui.theme.VpnErrorLight

@Composable
fun StatusCard(
    vpnState: VpnState,
    connectedTimeSec: Long,
    isDark: Boolean,
    latencyMs: Long?,
    isTestingLatency: Boolean,
    onTestLatency: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.CONNECTED -> if (isDark) VpnConnectedContainerDark else VpnConnectedContainerLight
            VpnState.CONNECTING -> if (isDark) VpnConnectingContainerDark else VpnConnectingContainerLight
            VpnState.ERROR -> if (isDark) VpnErrorContainerDark else VpnErrorContainerLight
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(600),
        label = "cardColor",
    )

    val buttonColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.CONNECTED -> if (isDark) VpnConnectedDark else VpnConnectedLight
            VpnState.CONNECTING -> if (isDark) VpnConnectingDark else VpnConnectingLight
            VpnState.ERROR -> if (isDark) VpnErrorDark else VpnErrorLight
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(600),
        label = "buttonColor",
    )

    // Pulse animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (vpnState == VpnState.CONNECTING) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Large circular toggle button
            Box(contentAlignment = Alignment.Center) {
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(88.dp)
                        .scale(scale)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            focusedBorderWidth = 4.dp,
                            focusedScale = 1.15f,
                        ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White,
                    ),
                    enabled = vpnState != VpnState.DISCONNECTING,
                ) {
                    when (vpnState) {
                        VpnState.DISCONNECTED -> Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "连接",
                            modifier = Modifier.size(40.dp),
                        )
                        VpnState.CONNECTED -> Icon(
                            Icons.Default.Stop,
                            contentDescription = "断开",
                            modifier = Modifier.size(40.dp),
                        )
                        VpnState.ERROR -> Icon(
                            Icons.Default.Warning,
                            contentDescription = "错误",
                            modifier = Modifier.size(40.dp),
                        )
                        VpnState.CONNECTING, VpnState.DISCONNECTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color.White,
                                strokeWidth = 3.dp,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State text
            Text(
                text = when (vpnState) {
                    VpnState.CONNECTED -> "已连接"
                    VpnState.DISCONNECTED -> "未连接"
                    VpnState.CONNECTING -> "连接中…"
                    VpnState.DISCONNECTING -> "断开中…"
                    VpnState.ERROR -> "连接错误"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Connected time
            if (vpnState == VpnState.CONNECTED && connectedTimeSec > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(connectedTimeSec),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Latency test chip when disconnected / error
            if (vpnState == VpnState.DISCONNECTED || vpnState == VpnState.ERROR) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = onTestLatency,
                    modifier = Modifier.tvFocusHighlight(
                        shape = AssistChipDefaults.shape,
                        focusedBorderWidth = 2.dp,
                        focusedScale = 1.08f,
                    ),
                    label = {
                        Text(
                            text = when {
                                isTestingLatency -> "测试中…"
                                latencyMs == null -> "测试延迟"
                                latencyMs > 0 -> "$latencyMs ms"
                                else -> "超时 / 失败"
                            },
                        )
                    },
                    leadingIcon = {
                        if (isTestingLatency) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when {
                            latencyMs != null && latencyMs > 0 -> if (latencyMs < 300) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                            latencyMs != null && latencyMs <= 0 -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        labelColor = when {
                            latencyMs != null && latencyMs > 0 -> if (latencyMs < 300) Color(0xFF2E7D32) else Color(0xFFF57F17)
                            latencyMs != null && latencyMs <= 0 -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    ),
                    enabled = !isTestingLatency,
                )
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
