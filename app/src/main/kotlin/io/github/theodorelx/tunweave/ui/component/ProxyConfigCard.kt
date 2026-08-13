package io.github.theodorelx.tunweave.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.theodorelx.tunweave.data.ProxyConfig

@Composable
fun ProxyConfigCard(
    config: ProxyConfig,
    isConnected: Boolean,
    showAuth: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "代理配置",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // The bundled native engine implements SOCKS5 only.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(config.proxyType.displayName) },
                    enabled = false,
                )
            }

            // Host
            OutlinedTextField(
                value = config.proxyHost,
                onValueChange = onHostChange,
                label = { Text("服务器地址") },
                placeholder = { Text("例如: 192.168.1.100") },
                singleLine = true,
                enabled = !isConnected,
                modifier = Modifier.fillMaxWidth(),
            )

            // Port
            OutlinedTextField(
                value = if (config.proxyPort > 0) config.proxyPort.toString() else "",
                onValueChange = onPortChange,
                label = { Text("端口") },
                placeholder = { Text("1080") },
                singleLine = true,
                enabled = !isConnected,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // Auth toggle
            TextButton(onClick = onToggleAuth) {
                Text(
                    text = if (showAuth) "认证信息 ▲" else "认证信息 ▼",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // Auth fields
            AnimatedVisibility(
                visible = showAuth,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = config.username,
                        onValueChange = onUsernameChange,
                        label = { Text("用户名") },
                        singleLine = true,
                        enabled = !isConnected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = config.password,
                        onValueChange = onPasswordChange,
                        label = { Text("密码") },
                        singleLine = true,
                        enabled = !isConnected,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility
                                        else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
