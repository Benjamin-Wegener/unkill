package com.example.unkill.services

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
import com.example.unkill.R
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unkill Service 3",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
                    delay(15000) // Monitor every 15 seconds (different timing)

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
        // TODO: Implement app monitoring logic
        // This will check if protected apps are still running
        // and restart them if they've been killed
        Log.d(TAG, "Monitoring protected apps")
    }

    private fun checkOtherServices() {
        // TODO: Implement inter-service communication
        // Check if other UnkillService instances are running
        // Restart them if they've been killed
        Log.d(TAG, "Checking other service instances")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnkillService3 destroyed")
        isMonitoring = false
        scope.cancel()
    }
}
