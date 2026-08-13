package io.github.theodorelx.tunweave

import android.app.Application
import io.github.theodorelx.tunweave.data.ProxyRepository

class TunWeaveApp : Application() {

    val repository: ProxyRepository by lazy { ProxyRepository(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
