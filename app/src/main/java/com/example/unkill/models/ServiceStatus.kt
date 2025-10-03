package com.example.unkill.models

enum class ServiceState {
    RUNNING,
    STOPPED,
    RESTARTING,
    IDLE
}

data class ServiceStatus(
    val serviceId: Int,
    val state: ServiceState,
    val startTime: Long = System.currentTimeMillis(), // Start time of current run
    val memoryUsage: Long = 0L,
    val isMonitoring: Boolean = false,
    val protectedAppsCount: Int = 0,
    val accumulatedUptime: Long = 0L  // Total accumulated uptime across all runs
) {
    val currentUptime: Long
        get() = if (state == ServiceState.RUNNING) {
            System.currentTimeMillis() - startTime  // Calculate current uptime if running
        } else {
            0L  // No current uptime when stopped
        }
    
    val totalUptime: Long
        get() = accumulatedUptime + currentUptime  // Total uptime (past + current)

    val formattedMemoryUsage: String
        get() = when {
            memoryUsage > 0 -> "${memoryUsage / 1024}KB"
            state == ServiceState.RUNNING -> "Calculating..."
            else -> "0KB"
        }

    val formattedUptime: String
        get() = when {
            totalUptime > 0 -> {
                val seconds = totalUptime / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                when {
                    hours > 0 -> "${hours}h ${minutes % 60}m"
                    minutes > 0 -> "${minutes}m ${seconds % 60}s"
                    else -> "${seconds}s"
                }
            }
            state == ServiceState.RUNNING -> "Starting..."
            else -> "0s"
        }
}
