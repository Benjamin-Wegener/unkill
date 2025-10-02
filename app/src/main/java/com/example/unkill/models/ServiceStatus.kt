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
    val startTime: Long = System.currentTimeMillis(),
    val memoryUsage: Long = 0L,
    val isMonitoring: Boolean = false,
    val protectedAppsCount: Int = 0
) {
    val uptime: Long
        get() = if (state == ServiceState.RUNNING) {
            System.currentTimeMillis() - startTime
        } else 0L

    val formattedMemoryUsage: String
        get() = if (memoryUsage > 0) {
            "${memoryUsage / 1024}KB"
        } else "N/A"

    val formattedUptime: String
        get() = if (uptime > 0) {
            val seconds = uptime / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            when {
                hours > 0 -> "${hours}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        } else "N/A"
}
