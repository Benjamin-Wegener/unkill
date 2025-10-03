package com.example.unkill.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {

    private val RUNTIME_PERMISSIONS = arrayOf(
        android.Manifest.permission.WAKE_LOCK,
        android.Manifest.permission.FOREGROUND_SERVICE
    )

    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()

        // Check runtime permissions (excluding QUERY_ALL_PACKAGES which is deprecated)
        for (permission in RUNTIME_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }

        // Check special permissions
        if (!hasUsageStatsPermission(context)) {
            missingPermissions.add("android.permission.PACKAGE_USAGE_STATS")
        }

        if (!hasIgnoreBatteryOptimizationPermission(context)) {
            missingPermissions.add("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
        }

        return missingPermissions
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid,
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun hasIgnoreBatteryOptimizationPermission(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Assume granted on older versions
        }
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        try {
            val intent = android.content.Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun requestUsageStatsPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun areAllPermissionsGranted(context: Context): Boolean {
        return getMissingPermissions(context).isEmpty()
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            android.Manifest.permission.WAKE_LOCK ->
                "Required to keep services running when device is idle"
            android.Manifest.permission.FOREGROUND_SERVICE ->
                "Required to run persistent background services"
            android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ->
                "Required to prevent battery optimization from killing services"
            "android.permission.PACKAGE_USAGE_STATS" ->
                "Required to monitor app usage patterns"
            "Usage Stats Permission" ->
                "Required to monitor app usage patterns"
            "Ignore Battery Optimization" ->
                "Required to prevent battery optimization from killing services"
            else -> "Required for proper functionality"
        }
    }
}
