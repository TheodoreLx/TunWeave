package com.simpleproxy.tun2socks

import io.github.theodorelx.tunweave.util.AppLogger

/**
 * Stable JNI ABI bridge for the checked-in hev-socks5-tunnel libraries.
 *
 * Keep this binary class name aligned with scripts/build-native.sh. It is
 * intentionally independent from the Android application ID and public brand.
 */
object Tun2socksJni {
    private const val TAG = "Tun2socksJni"

    var isLibraryLoaded = false
        private set

    init {
        try {
            System.loadLibrary("hev-socks5-tunnel")
            isLibraryLoaded = true
            AppLogger.i(TAG, "原生 libhev-socks5-tunnel.so 动态库已成功加载！")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            AppLogger.w(TAG, "未找到 libhev-socks5-tunnel.so 动态库 (${e.message})。")
        }
    }

    @JvmStatic
    external fun TProxyStartService(configPath: String, fd: Int): Boolean

    @JvmStatic
    external fun TProxyStopService(): Boolean

    @JvmStatic
    external fun TProxyIsRunning(): Boolean

    @JvmStatic
    external fun TProxyGetStats(): LongArray?
}
