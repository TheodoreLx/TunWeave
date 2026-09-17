package io.github.theodorelx.tunweave

import android.app.Application
import android.content.ComponentCallbacks2
import io.github.theodorelx.tunweave.data.ProxyRepository
import io.github.theodorelx.tunweave.util.AppLogger

class TunWeaveApp : Application() {

    val repository: ProxyRepository by lazy { ProxyRepository(this) }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            AppLogger.trimForBackground()
        }
    }
}
