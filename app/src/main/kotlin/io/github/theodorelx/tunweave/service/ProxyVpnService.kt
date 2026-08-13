package io.github.theodorelx.tunweave.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.theodorelx.tunweave.MainActivity
import io.github.theodorelx.tunweave.R
import io.github.theodorelx.tunweave.TunWeaveApp
import io.github.theodorelx.tunweave.data.Ipv6Mode
import io.github.theodorelx.tunweave.data.NetworkAddressParser
import io.github.theodorelx.tunweave.data.PerAppMode
import io.github.theodorelx.tunweave.data.ProxyConfig
import io.github.theodorelx.tunweave.data.TrafficStats
import io.github.theodorelx.tunweave.data.VpnState
import io.github.theodorelx.tunweave.tun2socks.Tun2socksManager
import io.github.theodorelx.tunweave.tun2socks.TunnelAddresses
import io.github.theodorelx.tunweave.ui.component.formatBytes
import io.github.theodorelx.tunweave.ui.component.formatSpeed
import io.github.theodorelx.tunweave.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.Inet6Address

class ProxyVpnService : VpnService() {

    companion object {
        private const val TAG = "ProxyVpnService"

        const val ACTION_CONNECT = "io.github.theodorelx.tunweave.CONNECT"
        const val ACTION_DISCONNECT = "io.github.theodorelx.tunweave.DISCONNECT"

        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "vpn_channel"

        val state = MutableStateFlow(VpnState.DISCONNECTED)
        val trafficStats = MutableStateFlow(TrafficStats())

        fun isRunning(): Boolean =
            state.value == VpnState.CONNECTED || state.value == VpnState.CONNECTING

        fun buildStartIntent(context: Context): Intent =
            Intent(context, ProxyVpnService::class.java).apply {
                action = ACTION_CONNECT
            }

        fun buildStopIntent(context: Context): Intent =
            Intent(context, ProxyVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private val tun2socks = Tun2socksManager()
    private val trafficMonitor = TrafficMonitor()
    private var monitorJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { (application as TunWeaveApp).repository }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(TAG, "ProxyVpnService 服务 onCreate 创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.d(TAG, "收到 Intent Action: ${intent?.action}")
        when (intent?.action) {
            ACTION_CONNECT -> loadConfigAndStart(requireAutoReconnect = false)
            ACTION_DISCONNECT -> {
                stopVpn()
            }
            SERVICE_INTERFACE -> loadConfigAndStart(requireAutoReconnect = false)
            null -> loadConfigAndStart(requireAutoReconnect = true)
            else -> {
                AppLogger.w(TAG, "忽略未知的服务 Action: ${intent.action}")
                stopSelf(startId)
            }
        }
        return START_STICKY
    }

    private fun loadConfigAndStart(requireAutoReconnect: Boolean) {
        if (isRunning()) return
        startForegroundNotification(getString(R.string.vpn_connecting))
        state.value = VpnState.CONNECTING
        serviceScope.launch {
            try {
                val config = repository.configFlow.first()
                if (requireAutoReconnect && !config.autoReconnect) {
                    AppLogger.i(TAG, "系统重启服务，但自动重连已关闭")
                    state.value = VpnState.DISCONNECTED
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                startVpn(config)
            } catch (e: Exception) {
                handleStartFailure("读取代理配置失败", e)
            }
        }
    }

    private fun startVpn(config: ProxyConfig) {
        AppLogger.setSensitiveValues(listOf(config.password))
        AppLogger.i(TAG, "开始启动 VPN 服务... [代理服务器: ${config.proxyType}://${config.proxyHost}:${config.proxyPort}]")
        try {
            validateConfig(config)

            // Build TUN interface
            val builder = Builder()
                .setSession("TunWeave")
                .addAddress(TunnelAddresses.IPV4, TunnelAddresses.IPV4_PREFIX_LENGTH)
                .setMtu(config.mtu)
                .setBlocking(false)

            if (config.ipv6Mode == Ipv6Mode.PROXY) {
                builder.addAddress(TunnelAddresses.IPV6, TunnelAddresses.IPV6_PREFIX_LENGTH)
            } else if (config.ipv6Mode == Ipv6Mode.BYPASS) {
                builder.allowFamily(OsConstants.AF_INET6)
            }

            AppLogger.d(
                TAG,
                "设置 TUN 网卡: ${TunnelAddresses.IPV4}/${TunnelAddresses.IPV4_PREFIX_LENGTH}, " +
                    "IPv6=${config.ipv6Mode.displayName}, MTU=${config.mtu}",
            )

            configureDns(builder, config)
            configurePerAppRouting(builder, config)
            configureRoutes(builder, config)

            tunInterface = checkNotNull(builder.establish()) { "创建 TUN 虚拟网卡失败" }

            val fd = tunInterface!!.fd
            AppLogger.i(TAG, "TUN 虚拟网卡接口已成功建立! 文件描述符 fd=$fd")

            check(tun2socks.start(this, fd, config)) { "原生 HEV 引擎启动失败" }

            // Start traffic monitoring and notification updates
            val nm = getSystemService(NotificationManager::class.java)
            val initialNativeStats = tun2socks.getTrafficStats()
            trafficMonitor.start(
                uploadBytes = initialNativeStats?.txBytes ?: 0L,
                downloadBytes = initialNativeStats?.rxBytes ?: 0L,
            )
            monitorJob = serviceScope.launch {
                while (true) {
                    delay(1000)
                    val nativeStats = tun2socks.getTrafficStats()
                    val snapshot = trafficMonitor.snapshot(
                        uploadBytes = nativeStats?.txBytes,
                        downloadBytes = nativeStats?.rxBytes,
                    )
                    trafficStats.value = snapshot

                    if (state.value == VpnState.CONNECTED) {
                        nm.notify(NOTIFICATION_ID, buildNotification(getString(R.string.vpn_connected), snapshot))
                    }
                }
            }

            state.value = VpnState.CONNECTED
            nm.notify(NOTIFICATION_ID, buildNotification(getString(R.string.vpn_connected), trafficStats.value))

            AppLogger.i(TAG, "VPN 服务建立完成并已成功进入运行状态！")

        } catch (e: Exception) {
            handleStartFailure("启动 VPN 失败", e)
        }
    }

    private fun validateConfig(config: ProxyConfig) {
        require(config.proxyHost.isNotBlank()) { "SOCKS5 服务器地址不能为空" }
        require(config.proxyPort in 1..65535) { "SOCKS5 服务器端口无效" }
        require(config.mtu in 1280..9000) { "MTU 必须在 1280..9000 范围内" }
        require(config.username.isEmpty() == config.password.isEmpty()) {
            "SOCKS5 用户名和密码必须同时填写或同时留空"
        }
        require(
            config.perAppMode != PerAppMode.WHITELIST ||
                config.selectedApps.any { it != packageName }
        ) {
            "白名单模式至少需要选择一个应用"
        }
        val dnsServers = listOf(config.dnsServer, config.dnsServerAlt)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        require(dnsServers.isNotEmpty()) { "至少需要配置一个 DNS 服务器" }
        dnsServers.forEach { server ->
            val address = NetworkAddressParser.parseNumericAddress(server)
            require(config.ipv6Mode != Ipv6Mode.BLOCK || address !is Inet6Address) {
                "IPv6 阻断模式不能使用 IPv6 DNS 服务器"
            }
        }
    }

    private fun configureDns(builder: Builder, config: ProxyConfig) {
        listOf(config.dnsServer, config.dnsServerAlt)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEachIndexed { index, server ->
                val address = NetworkAddressParser.parseNumericAddress(server)
                builder.addDnsServer(address)
                AppLogger.d(TAG, "设置${if (index == 0) "主" else "备用"} DNS: ${address.hostAddress}")
            }
    }

    private fun startForegroundNotification(statusText: String) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(statusText),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    private fun handleStartFailure(message: String, error: Exception) {
        AppLogger.e(TAG, "$message: ${error.message}", error)
        cleanupResources()
        state.value = VpnState.ERROR
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun configurePerAppRouting(builder: Builder, config: ProxyConfig) {
        AppLogger.i(TAG, "应用分流模式: ${config.perAppMode.displayName}, 已勾选应用数: ${config.selectedApps.size}")
        val selfPackage = packageName
        when (config.perAppMode) {
            PerAppMode.WHITELIST -> {
                for (pkg in config.selectedApps - selfPackage) {
                    try {
                        builder.addAllowedApplication(pkg)
                        AppLogger.d(TAG, "分流白名单生效: 允许包名 $pkg")
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "无法配置白名单应用包名 $pkg: ${e.message}")
                    }
                }
            }
            PerAppMode.BLACKLIST -> {
                for (pkg in config.selectedApps + selfPackage) {
                    try {
                        builder.addDisallowedApplication(pkg)
                        AppLogger.d(TAG, "分流黑名单生效: 绕过包名 $pkg")
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "无法配置黑名单应用包名 $pkg: ${e.message}")
                    }
                }
            }
            PerAppMode.DISABLED -> {
                builder.addDisallowedApplication(selfPackage)
                AppLogger.d(TAG, "全局代理模式: 已排除 TunWeave 自身，防止代理套接字回流 VPN")
            }
        }
    }

    private fun configureRoutes(builder: Builder, config: ProxyConfig) {
        builder.addRoute("0.0.0.0", 0)
        if (config.ipv6Mode == Ipv6Mode.PROXY) {
            builder.addRoute("::", 0)
        }

        if (config.bypassLan && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppLogger.d(
                TAG,
                "开启绕过局域网功能 (Android 13+ IpPrefix excludeRoute, IPv6=${config.ipv6Mode.displayName})",
            )

            val cidrs = config.bypassAddresses
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (cidr in cidrs) {
                try {
                    val parsed = NetworkAddressParser.parseCidr(cidr)
                    if (parsed.networkAddress is Inet6Address && config.ipv6Mode != Ipv6Mode.PROXY) {
                        continue
                    }
                    if (parsed.networkAddress.isLoopbackAddress) {
                        AppLogger.d(TAG, "忽略无需配置的回环绕过路由: $cidr")
                        continue
                    }
                    builder.excludeRoute(IpPrefix(parsed.networkAddress, parsed.prefixLength))
                    AppLogger.d(
                        TAG,
                        "已添加局域网绕过路由: ${parsed.networkAddress.hostAddress}/${parsed.prefixLength}",
                    )
                } catch (e: Exception) {
                    AppLogger.w(TAG, "解析绕过 CIDR 失败: $cidr (${e.message})")
                }
            }
        } else {
            if (config.bypassLan) {
                AppLogger.w(TAG, "Android 13 以下不支持 excludeRoute，局域网绕过未生效")
            }
            AppLogger.d(
                TAG,
                "添加默认路由: 0.0.0.0/0" +
                    if (config.ipv6Mode == Ipv6Mode.PROXY) ", ::/0" else "",
            )
        }
    }

    private fun stopVpn() {
        AppLogger.i(TAG, "正在断开 VPN 服务...")
        state.value = VpnState.DISCONNECTING

        cleanupResources()
        state.value = VpnState.DISCONNECTED

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        AppLogger.i(TAG, "VPN 服务已完全断开与注销")
    }

    override fun onRevoke() {
        AppLogger.w(TAG, "收到系统通知: 用户或系统撤销了 VPN 授权")
        stopVpn()
    }

    override fun onDestroy() {
        AppLogger.d(TAG, "ProxyVpnService onDestroy 销毁")
        cleanupResources()
        if (state.value != VpnState.ERROR) {
            state.value = VpnState.DISCONNECTED
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun cleanupResources() {
        monitorJob?.cancel()
        monitorJob = null
        tun2socks.stop()
        try {
            tunInterface?.close()
            if (tunInterface != null) {
                AppLogger.d(TAG, "TUN 虚拟网卡接口已关闭")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭 TUN 接口时出现异常: ${e.message}")
        }
        tunInterface = null
        trafficMonitor.reset()
        trafficStats.value = TrafficStats()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(statusText: String, stats: TrafficStats? = null): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ProxyVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (stats != null) {
            val up = formatSpeed(stats.uploadSpeed)
            val down = formatSpeed(stats.downloadSpeed)
            val total = formatBytes(stats.totalUpload + stats.totalDownload)
            "↑ $up  ↓ $down | 已用 $total"
        } else {
            statusText
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_rocket)
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, getString(R.string.action_disconnect), stopPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
