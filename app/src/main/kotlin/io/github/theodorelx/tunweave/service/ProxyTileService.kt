package io.github.theodorelx.tunweave.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import io.github.theodorelx.tunweave.MainActivity
import io.github.theodorelx.tunweave.R
import io.github.theodorelx.tunweave.TunWeaveApp
import io.github.theodorelx.tunweave.data.VpnState
import io.github.theodorelx.tunweave.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProxyTileService : TileService() {

    companion object {
        private const val TAG = "ProxyTileService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        AppLogger.d(TAG, "Quick Settings 磁贴开始监听状态变化")
        updateTileState(ProxyVpnService.state.value)

        stateJob?.cancel()
        stateJob = scope.launch {
            ProxyVpnService.state.collect { vpnState ->
                updateTileState(vpnState)
            }
        }
    }

    override fun onStopListening() {
        AppLogger.d(TAG, "Quick Settings 磁贴停止监听")
        stateJob?.cancel()
        stateJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        AppLogger.i(TAG, "用户点击了通知栏 Quick Settings 快捷磁贴")

        if (ProxyVpnService.isRunning()) {
            val stopIntent = ProxyVpnService.buildStopIntent(this)
            startService(stopIntent)
            AppLogger.i(TAG, "磁贴触发: 请求断开 VPN")
        } else {
            val prepareIntent = VpnService.prepare(this)
            if (prepareIntent != null) {
                val activityIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("request_vpn_connect", true)
                }
                openPermissionActivity(activityIntent)
                AppLogger.i(TAG, "磁贴触发: 需要 VPN 权限，拉起 MainActivity 授权页面")
            } else {
                scope.launch {
                    try {
                        val app = applicationContext as TunWeaveApp
                        val config = app.repository.configFlow.first()
                        val startIntent = ProxyVpnService.buildStartIntent(this@ProxyTileService)
                        ContextCompat.startForegroundService(this@ProxyTileService, startIntent)
                        AppLogger.i(TAG, "磁贴触发: 使用已保存配置直接启动 VPN (${config.proxyHost}:${config.proxyPort})")
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "磁贴触发: 启动 VPN 失败", e)
                    }
                }
            }
        }
    }

    private fun updateTileState(vpnState: VpnState) {
        val tile = qsTile ?: return
        when (vpnState) {
            VpnState.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "已连接"
                tile.setSubtitleCompat("TunWeave")
            }
            VpnState.DISCONNECTED -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "TunWeave"
                tile.setSubtitleCompat("点击连接")
            }
            VpnState.ERROR -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "TunWeave"
                tile.setSubtitleCompat("连接错误")
            }
            VpnState.CONNECTING, VpnState.DISCONNECTING -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = "TunWeave"
                tile.setSubtitleCompat(if (vpnState == VpnState.CONNECTING) "连接中…" else "断开中…")
            }
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_rocket)
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun openPermissionActivity(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    private fun Tile.setSubtitleCompat(value: CharSequence) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subtitle = value
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
