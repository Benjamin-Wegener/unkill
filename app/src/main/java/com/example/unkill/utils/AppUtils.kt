package com.example.unkill.utils

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import com.example.unkill.models.AppInfo
import java.util.*

object AppUtils {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val apps = mutableListOf<AppInfo>()

        try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

            for (packageInfo in packages) {
                if (packageInfo.packageName == context.packageName) continue

                val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                val icon = packageInfo.applicationInfo.loadIcon(packageManager)
                val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                apps.add(AppInfo(
                    packageName = packageInfo.packageName,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystemApp
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return apps.sortedBy { it.appName.lowercase() }
    }

    fun getUsageStatsApps(context: Context): List<AppInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7) // Last 7 days

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        return usageStats
            .filter { it.totalTimeInForeground > 0 }
            .mapNotNull { stats ->
                try {
                    val appInfo = getAppInfoFromPackageName(context, stats.packageName)
                    appInfo?.let { AppInfo(
                        packageName = it.packageName,
                        appName = it.appName,
                        icon = it.icon,
                        isSystemApp = it.isSystemApp
                    )}
                } catch (e: Exception) {
                    null
                }
            }
            .sortedByDescending { getAppUsageTime(usageStats, it.packageName) }
    }

    private fun getAppInfoFromPackageName(context: Context, packageName: String): AppInfo? {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = appInfo.loadLabel(packageManager).toString()
            val icon = appInfo.loadIcon(packageManager)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            AppInfo(
                packageName = packageName,
                appName = appName,
                icon = icon,
                isSystemApp = isSystemApp
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getAppUsageTime(usageStats: List<UsageStats>, packageName: String): Long {
        return usageStats.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun canAccessPackage(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun formatAppSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes >= 1024 * 1024 -> "%.1f MB".format(sizeInBytes.toFloat() / (1024 * 1024))
            sizeInBytes >= 1024 -> "%.1f KB".format(sizeInBytes.toFloat() / 1024)
            else -> "$sizeInBytes B"
        }
    }
}
