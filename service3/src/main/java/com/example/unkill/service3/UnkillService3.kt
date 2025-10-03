package com.example.unkillservice3

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class UnkillService3 : Service() {

    companion object {
        const val TAG = "UnkillService3"
        const val NOTIFICATION_ID = 1003
        const val CHANNEL_ID = "unkill_service_3"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UnkillService3 created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UnkillService3 started")
        startForeground(NOTIFICATION_ID, createNotification())
        if (!isMonitoring) {
            isMonitoring = true
            startMonitoring()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Unkill Service 3", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Unkill Service Instance 3"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unkill Service 3")
            .setContentText("Protecting your apps from being killed")
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive && isMonitoring) {
                try {
                    monitorProtectedApps()
                    checkOtherServices()
                    delay(10000)
                } catch (e: CancellationException) {
                    Log.d(TAG, "Monitoring cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error during monitoring", e)
                }
            }
        }
    }

    private fun monitorProtectedApps() {
        Log.d(TAG, "Monitoring protected apps")
        
        try {
            // Get protected apps list from main app's shared preferences
            val mainAppPackageName = "com.example.unkill"
            val sharedPreferences = this.createPackageContext(mainAppPackageName, Context.CONTEXT_IGNORE_SECURITY)
                ?.getSharedPreferences("unkill_prefs", Context.MODE_PRIVATE)
            
            val protectedApps = sharedPreferences?.getStringSet("protected_apps", emptySet())?.toList() ?: emptyList()
            
            Log.d(TAG, "Found ${protectedApps.size} protected apps to monitor")
            
            for (packageName in protectedApps) {
                if (!isAppRunning(packageName)) {
                    Log.d(TAG, "Protected app $packageName is not running, attempting to restart")
                    restartApp(packageName)
                } else {
                    Log.d(TAG, "Protected app $packageName is running")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring protected apps: ${e.message}")
        }
    }
    
    private fun isAppRunning(packageName: String): Boolean {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningProcesses = am.runningAppProcesses ?: return false
            
            for (processInfo in runningProcesses) {
                if (processInfo.processName == packageName) {
                    // Check if any of the pkgList contains our package
                    for (pkg in processInfo.pkgList) {
                        if (pkg == packageName) {
                            return true
                        }
                    }
                }
            }
            
            // Alternative method: Check with usage stats if available
            return isAppInUsageStats(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is running: ${e.message}")
            return true // Assume it's running to be safe
        }
    }
    
    private fun isAppInUsageStats(packageName: String): Boolean {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.HOUR, -1) // Check past hour
            
            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                System.currentTimeMillis()
            )
            
            stats.any { it.packageName == packageName && (System.currentTimeMillis() - it.lastTimeUsed) < 60000 } // Used in last minute
        } catch (e: Exception) {
            false
        }
    }
    
    private fun restartApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                Log.d(TAG, "Successfully started app: $packageName")
            } else {
                Log.w(TAG, "No launch intent found for app: $packageName")
                
                // Alternative: Try to start main activity directly
                val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(packageName)
                }
                
                val activities = packageManager.queryIntentActivities(mainIntent, 0)
                if (activities.isNotEmpty()) {
                    val activityInfo = activities.first().activityInfo
                    val targetedIntent = Intent().apply {
                        component = android.content.ComponentName(
                            activityInfo.packageName,
                            activityInfo.name
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(targetedIntent)
                    Log.d(TAG, "Started app via main activity: $packageName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting app $packageName: ${e.message}")
        }
    }

    private fun checkOtherServices() {
        Log.d(TAG, "Checking other service instances")
        
        // Check service 1
        if (!isServiceRunning("com.example.unkillservice1", "com.example.unkillservice1.UnkillService1")) {
            Log.d(TAG, "Service 1 is not running, attempting to restart")
            restartService("com.example.unkillservice1.UnkillService1", "com.example.unkillservice1")
        }
        
        // Check service 2
        if (!isServiceRunning("com.example.unkillservice2", "com.example.unkillservice2.UnkillService2")) {
            Log.d(TAG, "Service 2 is not running, attempting to restart")
            restartService("com.example.unkillservice2.UnkillService2", "com.example.unkillservice2")
        }
        
        // Check service 4
        if (!isServiceRunning("com.example.unkillservice4", "com.example.unkillservice4.UnkillService4")) {
            Log.d(TAG, "Service 4 is not running, attempting to restart")
            restartService("com.example.unkillservice4.UnkillService4", "com.example.unkillservice4")
        }
        
        // Check service 5
        if (!isServiceRunning("com.example.unkillservice5", "com.example.unkillservice5.UnkillService5")) {
            Log.d(TAG, "Service 5 is not running, attempting to restart")
            restartService("com.example.unkillservice5.UnkillService5", "com.example.unkillservice5")
        }
    }
    
    private fun isServiceRunning(packageName: String, serviceName: String): Boolean {
        try {
            // Check if the service package is installed
            packageManager.getPackageInfo(packageName, 0)
            
            // Try to start the service to check if it's available
            val intent = Intent().apply {
                setClassName(packageName, serviceName)
                action = "com.example.unkill.CHECK_SERVICE"
            }
            
            // We can't directly check if service is running from another app, 
            // but we can attempt to bind or send a command to it
            val resolveInfo = packageManager.resolveService(intent, 0)
            return resolveInfo != null
        } catch (e: Exception) {
            Log.d(TAG, "Service $packageName not available: ${e.message}")
            return false
        }
    }
    
    private fun restartService(serviceClassName: String, servicePackageName: String) {
        try {
            // Create intent to start the service
            val intent = Intent().apply {
                setClassName(servicePackageName, serviceClassName)
                action = "com.example.unkill.START_SERVICE"
            }
            
            // Start the service
            startService(intent)
            Log.d(TAG, "Started service: $serviceClassName in $servicePackageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service $serviceClassName: ${e.message}")
            
            // Fallback: try using package manager to start the service
            try {
                val pm = packageManager
                val intent = Intent().apply {
                    setClassName(servicePackageName, serviceClassName)
                    action = "android.intent.action.MAIN"
                    addCategory("android.intent.category.LAUNCHER")
                }
                
                // If the service is in a separate APK, it should be installed
                // and we can try to start it using the package name
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to start service using fallback: ${e2.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnkillService3 destroyed")
        isMonitoring = false
        scope.cancel()
    }
}
