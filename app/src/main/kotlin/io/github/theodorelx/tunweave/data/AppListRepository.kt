package io.github.theodorelx.tunweave.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackageName = context.packageName

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
        packages
            .filter { it.packageName != selfPackageName }
            .map { appInfo ->
                val name = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = try {
                    pm.getApplicationIcon(appInfo)
                } catch (_: Exception) {
                    null
                }
                AppInfo(
                    appName = name,
                    packageName = appInfo.packageName,
                    icon = icon,
                    isSystemApp = isSystem,
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}
