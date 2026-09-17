package io.github.theodorelx.tunweave.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListRepository(context: Context) {

    companion object {
        private const val DEFAULT_ICON_SIZE_PX = 96
        private const val ICON_CACHE_BYTES = 2 * 1024 * 1024
    }

    private val appContext = context.applicationContext
    private val iconCache = object : LruCache<String, Bitmap>(ICON_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        val selfPackageName = appContext.packageName

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
        packages
            .filter { it.packageName != selfPackageName }
            .map { appInfo ->
                val name = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                AppInfo(
                    appName = name,
                    packageName = appInfo.packageName,
                    isSystemApp = isSystem,
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    /** Loads and scales only icons requested by visible list rows. */
    suspend fun getAppIcon(
        packageName: String,
        sizePx: Int = DEFAULT_ICON_SIZE_PX,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val safeSize = sizePx.coerceIn(48, 192)
        val key = "$packageName@$safeSize"
        iconCache.get(key)?.let { return@withContext it }

        val bitmap = try {
            val drawable = appContext.packageManager.getApplicationIcon(packageName).mutate()
            Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888).also { output ->
                val canvas = Canvas(output)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } catch (_: Exception) {
            null
        }
        bitmap?.also { iconCache.put(key, it) }
    }

    fun clearIconCache() {
        iconCache.evictAll()
    }
}
