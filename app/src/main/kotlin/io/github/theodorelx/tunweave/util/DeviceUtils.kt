package io.github.theodorelx.tunweave.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceUtils {

    /**
     * Determines whether the current device is an Android TV / Leanback device.
     */
    fun isTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }

        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return true
        }

        @Suppress("DEPRECATION")
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
            return true
        }

        return false
    }
}
