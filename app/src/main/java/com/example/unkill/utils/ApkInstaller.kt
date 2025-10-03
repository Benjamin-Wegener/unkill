package com.example.unkill.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

class ApkInstaller(private val context: Context) {

    companion object {
        const val TAG = "ApkInstaller"
    }

    fun installApks(apkPaths: List<String>, callback: (Boolean, String) -> Unit) {
        try {
            // For demonstration, we'll just log the paths
            // In a real implementation, you would copy APKs to device and install them

            val successCount = apkPaths.size
            val message = "Would install $successCount service APKs. In production, this would use PackageInstaller API."
            Log.d(TAG, message)
            callback(true, message)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APKs", e)
            callback(false, "Installation failed: ${e.message}")
        }
    }

    fun buildServiceApk(serviceModule: String, callback: (Boolean, String?) -> Unit) {
        // In a real implementation, this would call gradle to build APK
        // For demo purposes, we'll simulate finding a prebuilt APK

        val apkPath = getSimulatedApkPath(serviceModule)
        callback(true, apkPath)
    }

    private fun getSimulatedApkPath(serviceModule: String): String {
        // In production, this would be an actual APK file path
        return "/data/local/tmp/$serviceModule.apk"
    }

    fun isServiceInstalled(serviceModule: String): Boolean {
        // In a real implementation, this would check if the service APK is installed
        // For demo purposes, we'll simulate that services are not installed initially
        return false
    }

    fun buildAndInstallAllServices(serviceModules: List<String>, callback: (Boolean, String) -> Unit) {
        try {
            val apkPaths = mutableListOf<String>()

            // Build all service APKs
            for (module in serviceModules) {
                val apkPath = getSimulatedApkPath(module)
                apkPaths.add(apkPath)
                Log.d(TAG, "Built APK for $module at $apkPath")
            }

            // Install all built APKs
            if (apkPaths.size == serviceModules.size) {
                installApks(apkPaths) { success, message ->
                    if (success) {
                        Log.d(TAG, "Successfully installed ${apkPaths.size} service APKs")
                    } else {
                        Log.e(TAG, "Failed to install service APKs: $message")
                    }
                    callback(success, message)
                }
            } else {
                val message = "Failed to build all service APKs (${apkPaths.size}/${serviceModules.size} built)"
                Log.e(TAG, message)
                callback(false, message)
            }

        } catch (e: Exception) {
            val message = "Error installing services: ${e.message}"
            Log.e(TAG, message, e)
            callback(false, message)
        }
    }
}
