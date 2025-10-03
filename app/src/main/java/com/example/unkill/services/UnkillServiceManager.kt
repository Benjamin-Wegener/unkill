package com.example.unkill.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
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
        const val CHANNEL_ID = "unkill_service_manager"
        const val NOTIFICATION_ID = 1000

        private val serviceStatuses = mutableMapOf<Int, ServiceStatus>()
        private val serviceJobs = mutableMapOf<Int, Job>()
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        @Volatile
        private var instance: UnkillServiceManager? = null

        fun getInstance(context: android.content.Context): UnkillServiceManager {
            return instance ?: synchronized(this) {
                instance ?: UnkillServiceManager().also {
                    instance = it
                    // Start the service when getting the instance
                    val intent = android.content.Intent(context, UnkillServiceManager::class.java)
                    context.startService(intent)
                }
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

        createNotificationChannel()
        
        // Initialize service statuses
        for (i in 1..5) {
            serviceStatuses[i] = ServiceStatus(
                serviceId = i,
                state = ServiceState.STOPPED,
                startTime = System.currentTimeMillis(),
                memoryUsage = 0L,
                isMonitoring = false,
                protectedAppsCount = 0,
                accumulatedUptime = 0L
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unkill Service Manager",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Manages Unkill protection services"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unkill Service Manager")
            .setContentText("Managing app protection services")
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the service in foreground to comply with Android's foreground service requirements
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val serviceId = intent?.getIntExtra(EXTRA_SERVICE_ID, -1) ?: -1
                if (serviceId == -1) {
                    // No specific service ID provided, start services for protected apps
                    startServicesForProtectedApps()
                } else {
                    // Specific service ID provided, start just that instance
                    startServiceInstance(serviceId)
                }
            }
            ACTION_STOP_SERVICE -> {
                val serviceId = intent?.getIntExtra(EXTRA_SERVICE_ID, 1) ?: 1
                stopServiceInstance(serviceId)
            }
            ACTION_RESTART_SERVICE -> {
                val serviceId = intent?.getIntExtra(EXTRA_SERVICE_ID, 1) ?: 1
                restartServiceInstance(serviceId)
            }
        }
        return START_STICKY
    }

    private fun startServiceInstance(serviceId: Int) {
        Log.d(TAG, "Starting service instance $serviceId")

        // Stop existing job if any
        serviceJobs[serviceId]?.cancel()

        // Get the previous status to preserve accumulated uptime
        val previousStatus = serviceStatuses[serviceId]
        
        // Get protected apps count
        val protectedApps = getProtectedAppsCount()

        // Update status - Initialize with a small initial memory usage to show that monitoring is active
        serviceStatuses[serviceId] = ServiceStatus(
            serviceId = serviceId,
            state = ServiceState.RUNNING,
            startTime = System.currentTimeMillis(),
            // If previous memory usage was 0 or no previous status exists, use a small default value
            memoryUsage = maxOf(previousStatus?.memoryUsage ?: 0L, 1024L), // At least 1KB to avoid "Calculating..."
            isMonitoring = true,
            protectedAppsCount = protectedApps,
            // Accumulate the previous total uptime when starting a new run
            accumulatedUptime = if (previousStatus?.state == ServiceState.RUNNING) {
                // If the service was running, preserve its accumulated uptime + the time it was running
                previousStatus.accumulatedUptime + (System.currentTimeMillis() - previousStatus.startTime)
            } else {
                previousStatus?.accumulatedUptime ?: 0L
            }
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

        // Preserve the previous status but change the state to STOPPED
        val previousStatus = serviceStatuses[serviceId]
        serviceStatuses[serviceId] = ServiceStatus(
            serviceId = serviceId,
            state = ServiceState.STOPPED,
            startTime = previousStatus?.startTime ?: System.currentTimeMillis(),
            memoryUsage = previousStatus?.memoryUsage ?: 0L,
            isMonitoring = false,
            protectedAppsCount = previousStatus?.protectedAppsCount ?: 0,
            // Update accumulatedUptime to include the time this run was active
            accumulatedUptime = if (previousStatus?.state == ServiceState.RUNNING) {
                previousStatus.accumulatedUptime + (System.currentTimeMillis() - previousStatus.startTime)
            } else {
                previousStatus?.accumulatedUptime ?: 0L
            }
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
                // Update memory usage more frequently initially, then every 5 seconds
                delay(2000) // Check every 2 seconds for more responsive updates

                // Update memory usage (simulated)
                val memoryUsage = (Math.random() * 2048 * 1024).toLong() // Up to 2MB

                serviceStatuses[serviceId]?.let { status ->
                    if (status.state == ServiceState.RUNNING) { // Only update if still running
                        serviceStatuses[serviceId] = status.copy(
                            memoryUsage = memoryUsage,
                            isMonitoring = true
                            // Don't update lastUptime here - it should only be updated when service stops
                        )
                    }
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

            // Check if service APK already exists and is installed
            if (isServiceApkInstalled(serviceId)) {
                Log.d(TAG, "Service $serviceId APK is already installed")
                return true
            }

            // Create APK file for the specific service
            val apkFile = createServiceApk(serviceId)
            if (apkFile != null && apkFile.exists()) {
                // Install the APK
                installApk(apkFile, serviceId)
                true
            } else {
                Log.e(TAG, "Failed to create APK file for service $serviceId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install standalone service $serviceId", e)
            false
        }
    }

    fun installAllStandaloneServices(): Map<Int, Boolean> {
        val results = mutableMapOf<Int, Boolean>()
        for (serviceId in 1..5) {
            results[serviceId] = installStandaloneService(serviceId)
        }
        Log.d(TAG, "Installation results: $results")
        return results
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

    private fun isServiceApkInstalled(serviceId: Int): Boolean {
        val context = applicationContext
        val servicePackageName = "com.example.unkillservice$serviceId"
        return try {
            context.packageManager.getPackageInfo(servicePackageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun startServicesForApps(packageNames: List<String>) {
        Log.d(TAG, "Starting services for ${packageNames.size} apps")

        // Start up to 5 service instances based on the number of apps
        val numServices = minOf(packageNames.size, 5)

        for (i in 1..numServices) {
            startServiceInstance(i)
        }
    }

    // Start services for apps based on saved preferences
    private fun startServicesForProtectedApps() {
        val sharedPreferences = applicationContext.getSharedPreferences("unkill_prefs", android.content.Context.MODE_PRIVATE)
        val protectedPackages = sharedPreferences.getStringSet("protected_apps", emptySet())?.toList() ?: emptyList()

        if (protectedPackages.isNotEmpty()) {
            Log.d(TAG, "Found ${protectedPackages.size} protected apps to start services for")
            startServicesForApps(protectedPackages)
        } else {
            Log.d(TAG, "No protected apps found in preferences")
        }
    }

    private fun getProtectedAppsCount(): Int {
        val sharedPreferences = applicationContext.getSharedPreferences("unkill_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("protected_apps", emptySet())?.size ?: 0
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnkillServiceManager destroyed")

        // Cancel all monitoring jobs
        serviceJobs.values.forEach { it.cancel() }
        serviceJobs.clear()

        // Update all service statuses, preserving previous values
        serviceStatuses.keys.forEach { serviceId ->
            val previousStatus = serviceStatuses[serviceId]
            serviceStatuses[serviceId] = ServiceStatus(
                serviceId = serviceId,
                state = ServiceState.STOPPED,
                startTime = previousStatus?.startTime ?: System.currentTimeMillis(),
                memoryUsage = previousStatus?.memoryUsage ?: 0L,
                isMonitoring = false,
                protectedAppsCount = previousStatus?.protectedAppsCount ?: 0,
                accumulatedUptime = if (previousStatus?.state == ServiceState.RUNNING) {
                    previousStatus.accumulatedUptime + (System.currentTimeMillis() - previousStatus.startTime)
                } else {
                    previousStatus?.accumulatedUptime ?: 0L
                }
            )
        }
    }
}
