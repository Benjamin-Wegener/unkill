package com.example.unkill.service2

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

class UnkillService2 : Service() {

    companion object {
        const val TAG = "UnkillService2"
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "unkill_service_2"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UnkillService2 created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UnkillService2 started")

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
                "Unkill Service 2",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Unkill Service Instance 2"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unkill Service 2")
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
    }

    private fun checkOtherServices() {
        Log.d(TAG, "Checking other service instances")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnkillService2 destroyed")
        isMonitoring = false
        scope.cancel()
    }
}
