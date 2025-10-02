package com.example.unkill.services

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.content.FileProvider
import com.example.unkill.models.ServiceState
import com.example.unkill.models.ServiceStatus
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class UnkillServiceManager : Service() {

    companion object {
        const val TAG = "UnkillServiceManager"
        const val ACTION_START_SERVICE = "com.example.unkill.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.unkill.STOP_SERVICE"
        const val ACTION_RESTART_SERVICE = "com.example.unkill.RESTART_SERVICE"
        const val EXTRA_SERVICE_ID = "service_id"

        private val serviceStatuses = mutableMapOf<Int, ServiceStatus>()
        private val serviceJobs = mutableMapOf<Int, Job>()
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        @Volatile
        private var instance: UnkillServiceManager? = null

        fun getInstance(context: android.content.Context): UnkillServiceManager {
            return instance ?: synchronized(this) {
                instance ?: UnkillServiceManager().also { instance = it }
            }
        }

        fun getServiceStatus(serviceId: Int): ServiceStatus? = serviceStatuses[serviceId]

        fun getAllServiceStatuses(): Map<Int, ServiceStatus> = serviceStatuses.toMap()

        fun isServiceRunning(serviceId: Int): Boolean {
            return serviceStatuses[serviceId]?.state == ServiceState.RUNNING
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UnkillServiceManager created")

        // Initialize service statuses
        for (i in 1..5) {
            serviceStatuses[i] = ServiceStatus(
                serviceId = i,
                state = ServiceState.STOPPED
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val serviceId = intent.getIntExtra(EXTRA_SERVICE_ID, 1)
                startServiceInstance(serviceId)
            }
            ACTION_STOP_SERVICE -> {
                val serviceId = intent.getIntExtra(EXTRA_SERVICE_ID, 1)
                stopServiceInstance(serviceId)
            }
            ACTION_RESTART_SERVICE -> {
                val serviceId = intent.getIntExtra(EXTRA_SERVICE_ID, 1)
                restartServiceInstance(serviceId)
            }
        }
        return START_STICKY
    }

    private fun startServiceInstance(serviceId: Int) {
        Log.d(TAG, "Starting service instance $serviceId")

        // Stop existing job if any
        serviceJobs[serviceId]?.cancel()

        // Update status
        serviceStatuses[serviceId] = ServiceStatus(
            serviceId = serviceId,
            state = ServiceState.RUNNING,
            startTime = System.currentTimeMillis()
        )

        // Start monitoring job
        val job = scope.launch {
            monitorServiceInstance(serviceId)
        }
        serviceJobs[serviceId] = job
    }

    private fun stopServiceInstance(serviceId: Int) {
        Log.d(TAG, "Stopping service instance $serviceId")

        // Cancel monitoring job
        serviceJobs[serviceId]?.cancel()
        serviceJobs.remove(serviceId)

        // Update status
        serviceStatuses[serviceId] = ServiceStatus(
            serviceId = serviceId,
            state = ServiceState.STOPPED
        )
    }

    private fun restartServiceInstance(serviceId: Int) {
        Log.d(TAG, "Restarting service instance $serviceId")
        stopServiceInstance(serviceId)
        startServiceInstance(serviceId)
    }

    private suspend fun monitorServiceInstance(serviceId: Int) {
        while (true) {
            try {
                // Simulate monitoring work
                delay(5000) // Check every 5 seconds

                // Update memory usage (simulated)
                val memoryUsage = (Math.random() * 2048 * 1024).toLong() // Up to 2MB

                serviceStatuses[serviceId]?.let { status ->
                    serviceStatuses[serviceId] = status.copy(
                        memoryUsage = memoryUsage,
                        isMonitoring = true
                    )
                }

                Log.d(TAG, "Service $serviceId monitoring - Memory: ${memoryUsage / 1024}KB")

            } catch (e: CancellationException) {
                Log.d(TAG, "Service $serviceId monitoring cancelled")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring service $serviceId", e)
            }
        }
    }

    fun installStandaloneService(serviceId: Int): Boolean {
        return try {
            Log.d(TAG, "Installing standalone service $serviceId")

            // Create APK file for the specific service
            val apkFile = createServiceApk(serviceId)
            if (apkFile != null) {
                // Install the APK
                installApk(apkFile, serviceId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install standalone service $serviceId", e)
            false
        }
    }

    private fun createServiceApk(serviceId: Int): File? {
        return try {
            val context = applicationContext
            val serviceClassName = "UnkillService$serviceId"

            // Create a minimal APK structure for the service
            val apkFile = File(context.cacheDir, "unkill_service_$serviceId.apk")

            // For now, create a placeholder file
            // In a real implementation, this would involve:
            // 1. Dynamic APK generation with only the specific service
            // 2. Different package name (e.g., com.example.unkillService1)
            // 3. Minimal manifest with only necessary permissions
            // 4. Signing the APK

            // Create a simple marker file for now
            FileOutputStream(apkFile).use { fos ->
                fos.write("Unkill Service $serviceId APK Placeholder".toByteArray())
            }

            Log.d(TAG, "Created APK file for service $serviceId: ${apkFile.absolutePath}")
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create APK for service $serviceId", e)
            null
        }
    }

    private fun installApk(apkFile: File, serviceId: Int) {
        try {
            val context = applicationContext
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }

            context.startActivity(installIntent)
            Log.d(TAG, "Started installation of service $serviceId APK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK for service $serviceId", e)
        }
    }

    fun checkInstalledServices(): Map<Int, Boolean> {
        val context = applicationContext
        val installedServices = mutableMapOf<Int, Boolean>()

        for (serviceId in 1..5) {
            val servicePackageName = "com.example.unkillservice$serviceId"
            installedServices[serviceId] = try {
                context.packageManager.getPackageInfo(servicePackageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        return installedServices
    }

    fun startServicesForApps(packageNames: List<String>) {
        Log.d(TAG, "Starting services for ${packageNames.size} apps")

        // Start up to 5 service instances based on the number of apps
        val numServices = minOf(packageNames.size, 5)

        for (i in 1..numServices) {
            startServiceInstance(i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnkillServiceManager destroyed")

        // Cancel all monitoring jobs
        serviceJobs.values.forEach { it.cancel() }
        serviceJobs.clear()

        // Update all service statuses
        serviceStatuses.keys.forEach { serviceId ->
            serviceStatuses[serviceId] = ServiceStatus(
                serviceId = serviceId,
                state = ServiceState.STOPPED
            )
        }
    }
}
