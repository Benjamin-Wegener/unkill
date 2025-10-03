package com.example.unkillservice1

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

class UnkillService1 : Service() {

    companion object {
        const val TAG = "UnkillService1"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "unkill_service_1"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UnkillService1 created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UnkillService1 started")

        startForeground(NOTIFICATION_ID, createNotification())

        if (!isMonitoring) {
            isMonitoring = true
            startMonitoring()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unkill Service 1",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Unkill Service Instance 1"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unkill Service 1")
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
                    // Monitor protected apps and system resources
                    monitorProtectedApps()

                    // Check if other service instances are running
                    checkOtherServices()

                    // Update notification if needed
                    delay(10000) // Monitor every 10 seconds

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
        try {
            val protectedApps = getProtectedAppsList()

            if (protectedApps.isEmpty()) {
                logToGlobalDebug("No protected apps configured")
                return
            }

            logToGlobalDebug("Monitoring ${protectedApps.size} protected apps: ${protectedApps.joinToString(", ")}")

            for (packageName in protectedApps) {
                val isRunning = isAppRunning(packageName)
                logToGlobalDebug("App check: $packageName - ${if (isRunning) "RUNNING" else "KILLED"}")

                if (!isRunning) {
                    logToGlobalDebug("🔄 DETECTED: App $packageName was killed - restarting...")
                    val restartSuccess = restartApp(packageName)
                    if (restartSuccess) {
                        logToGlobalDebug("✅ SUCCESS: Restarted $packageName")
                    } else {
                        logToGlobalDebug("❌ FAILED: Could not restart $packageName")
                    }
                }
            }
        } catch (e: Exception) {
            logToGlobalDebug("ERROR: Failed to monitor protected apps: ${e.message}")
            Log.e(TAG, "Error monitoring protected apps", e)
        }
    }

    private fun getProtectedAppsList(): List<String> {
        return try {
            // Try multiple methods to get the protected apps list

            // Method 1: Direct SharedPreferences access (if same process)
            try {
                val sharedPreferences = getSharedPreferences("unkill_prefs", Context.MODE_PRIVATE)
                val apps = sharedPreferences.getStringSet("protected_apps", emptySet())?.toList() ?: emptyList()
                if (apps.isNotEmpty()) {
                    logToGlobalDebug("Got ${apps.size} protected apps via direct SharedPreferences")
                    return apps
                }
            } catch (e: Exception) {
                logToGlobalDebug("Direct SharedPreferences failed: ${e.message}")
            }

            // Method 2: Via package context (cross-process)
            try {
                val mainAppPackageName = "com.example.unkill"
                val sharedPreferences = this.createPackageContext(mainAppPackageName, Context.CONTEXT_IGNORE_SECURITY)
                    ?.getSharedPreferences("unkill_prefs", Context.MODE_PRIVATE)

                val apps = sharedPreferences?.getStringSet("protected_apps", emptySet())?.toList() ?: emptyList()
                if (apps.isNotEmpty()) {
                    logToGlobalDebug("Got ${apps.size} protected apps via package context")
                    return apps
                }
            } catch (e: Exception) {
                logToGlobalDebug("Package context SharedPreferences failed: ${e.message}")
            }

            // Method 3: Check global debug log for recent app selections
            try {
                val globalPrefs = getSharedPreferences("unkill_global_debug", Context.MODE_PRIVATE)
                val globalLog = globalPrefs.getString("global_debug_log", "") ?: ""

                // Look for recent APP SELECTION entries
                val lines = globalLog.split("\n")
                val recentSelections = lines.filter { it.contains("APP SELECTION: Protected") }
                    .takeLast(3) // Get last 3 selections

                if (recentSelections.isNotEmpty()) {
                    logToGlobalDebug("Found ${recentSelections.size} recent app selections in global log")
                    // Extract package names from the log entries
                    val extractedApps = mutableListOf<String>()
                    recentSelections.forEach { line ->
                        // Parse lines like "APP SELECTION: Protected 2 apps: com.example.app1, com.example.app2"
                        val appsText = line.substringAfter("Protected").substringAfter("apps:")
                        val packageNames = appsText.split(",").map { it.trim() }
                        extractedApps.addAll(packageNames)
                    }

                    if (extractedApps.isNotEmpty()) {
                        logToGlobalDebug("Extracted ${extractedApps.size} apps from global log: ${extractedApps.joinToString(", ")}")
                        return extractedApps.distinct()
                    }
                }
            } catch (e: Exception) {
                logToGlobalDebug("Global log parsing failed: ${e.message}")
            }

            logToGlobalDebug("No protected apps found via any method")
            emptyList()

        } catch (e: Exception) {
            logToGlobalDebug("CRITICAL ERROR getting protected apps: ${e.message}")
            emptyList()
        }
    }
    
    private fun isAppRunning(packageName: String): Boolean {
        return try {
            // Method 1: Check running processes
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningProcesses = am.runningAppProcesses ?: return false

            for (processInfo in runningProcesses) {
                if (processInfo.processName == packageName) {
                    for (pkg in processInfo.pkgList) {
                        if (pkg == packageName) {
                            return true
                        }
                    }
                }
            }

            // Method 2: Check with usage stats (more reliable for recent activity)
            isAppInUsageStats(packageName)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is running: ${e.message}")
            // Don't assume it's running - be more aggressive about checking
            false
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
    
    private fun restartApp(packageName: String): Boolean {
        return try {
            logToGlobalDebug("Attempting to restart $packageName with multiple methods...")

            // Method 1: Standard launch intent
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(launchIntent)
                    logToGlobalDebug("✅ Method 1 SUCCESS: $packageName via launch intent")
                    return true
                }
            } catch (e: Exception) {
                logToGlobalDebug("Method 1 failed for $packageName: ${e.message}")
            }

            // Method 2: Main activity component intent
            try {
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
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(targetedIntent)
                    logToGlobalDebug("✅ Method 2 SUCCESS: $packageName via main activity")
                    return true
                }
            } catch (e: Exception) {
                logToGlobalDebug("Method 2 failed for $packageName: ${e.message}")
            }

            // Method 3: Try to get all launcher activities for the package
            try {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(packageName)
                }

                val resolveInfos = packageManager.queryIntentActivities(intent, 0)
                if (resolveInfos.isNotEmpty()) {
                    val resolveInfo = resolveInfos.first()
                    val activityName = resolveInfo.activityInfo.name
                    val componentName = android.content.ComponentName(packageName, activityName)

                    val startIntent = Intent(Intent.ACTION_MAIN).apply {
                        component = componentName
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }

                    startActivity(startIntent)
                    logToGlobalDebug("✅ Method 3 SUCCESS: $packageName via component name")
                    return true
                }
            } catch (e: Exception) {
                logToGlobalDebug("Method 3 failed for $packageName: ${e.message}")
            }

            // Method 4: Use ADB-like force restart (simulate user tap)
            try {
                val forceIntent = Intent().apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(forceIntent)
                logToGlobalDebug("✅ Method 4 SUCCESS: $packageName via force intent")
                return true
            } catch (e: Exception) {
                logToGlobalDebug("Method 4 failed for $packageName: ${e.message}")
            }

            logToGlobalDebug("❌ ALL METHODS FAILED: Could not restart $packageName")
            false
        } catch (e: Exception) {
            logToGlobalDebug("❌ CRITICAL ERROR restarting $packageName: ${e.message}")
            Log.e(TAG, "Critical error starting app $packageName", e)
            false
        }
    }

    private fun logToGlobalDebug(message: String) {
        try {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val logEntry = "[$timestamp] SERVICE1: $message\n"

            // Write to global debug shared preferences that MainActivity reads
            val sharedPreferences = getSharedPreferences("unkill_global_debug", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val existingLog = sharedPreferences.getString("global_debug_log", "")
            val updatedLog = if (existingLog.isNullOrEmpty()) {
                logEntry
            } else {
                existingLog + logEntry
            }

            // Keep only last 25 entries for services
            val lines = updatedLog.split("\n")
            val limitedLog = if (lines.size > 25) {
                lines.takeLast(25).joinToString("\n")
            } else {
                updatedLog
            }

            editor.putString("global_debug_log", limitedLog)
            editor.apply()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to global debug log: ${e.message}")
        }
    }

    private fun checkOtherServices() {
        Log.d(TAG, "Checking other service instances")
        
        // Check if other services are running by attempting to bind to them or query their status
        val servicePackage = this.packageName
        val pm = packageManager
        
        // Check service 2
        if (!isServiceRunning("com.example.unkillservice2", "com.example.unkillservice2.UnkillService2")) {
            Log.d(TAG, "Service 2 is not running, attempting to restart")
            restartService("com.example.unkillservice2.UnkillService2", "com.example.unkillservice2")
        }
        
        // Check service 3
        if (!isServiceRunning("com.example.unkillservice3", "com.example.unkillservice3.UnkillService3")) {
            Log.d(TAG, "Service 3 is not running, attempting to restart")
            restartService("com.example.unkillservice3.UnkillService3", "com.example.unkillservice3")
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
        Log.d(TAG, "UnkillService1 destroyed")
        isMonitoring = false
        scope.cancel()
    }
}
