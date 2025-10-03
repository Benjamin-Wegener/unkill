package com.example.unkill.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unkill.services.UnkillServiceManager
import com.example.unkill.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _serviceStatus = MutableStateFlow("Stopped")
    val serviceStatus: StateFlow<String> = _serviceStatus

    private val _activeInstances = MutableStateFlow(0)
    val activeInstances: StateFlow<Int> = _activeInstances

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _debugText = MutableStateFlow("Debug: App started\nStatus: Ready to initialize...")
    val debugText: StateFlow<String> = _debugText

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val context: Context = application

    fun checkRequiredPermissions() {
        viewModelScope.launch {
            val missingPermissions = PermissionUtils.getMissingPermissions(context)

            if (missingPermissions.isNotEmpty()) {
                _errorMessage.value = "Missing required permissions: ${missingPermissions.joinToString(", ")}"
            } else {
                _errorMessage.value = ""
            }
        }
    }

    fun startAllServices() {
        viewModelScope.launch {
            try {
                for (i in 1..5) {
                    val intent = android.content.Intent(context, UnkillServiceManager::class.java).apply {
                        action = UnkillServiceManager.ACTION_START_SERVICE
                        putExtra(UnkillServiceManager.EXTRA_SERVICE_ID, i)
                    }
                    context.startService(intent)
                }
                _serviceStatus.value = "Running"
                _activeInstances.value = 5
                _isServiceRunning.value = true
                _errorMessage.value = ""
                updateDebugText("All services started successfully")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start services: ${e.message}"
                updateDebugText("ERROR: Failed to start services: ${e.message}")
            }
        }
    }

    fun stopAllServices() {
        viewModelScope.launch {
            try {
                for (i in 1..5) {
                    val intent = android.content.Intent(context, UnkillServiceManager::class.java).apply {
                        action = UnkillServiceManager.ACTION_STOP_SERVICE
                        putExtra(UnkillServiceManager.EXTRA_SERVICE_ID, i)
                    }
                    context.stopService(intent)
                }
                _serviceStatus.value = "Stopped"
                _activeInstances.value = 0
                _isServiceRunning.value = false
                _errorMessage.value = ""
                updateDebugText("All services stopped")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop services: ${e.message}"
                updateDebugText("ERROR: Failed to stop services: ${e.message}")
            }
        }
    }

    fun toggleServices() {
        if (_isServiceRunning.value) {
            stopAllServices()
        } else {
            startAllServices()
        }
    }

    fun refreshServiceStatus() {
        viewModelScope.launch {
            val runningCount = (1..5).count { serviceId ->
                UnkillServiceManager.isServiceRunning(serviceId)
            }
            _activeInstances.value = runningCount
            _serviceStatus.value = if (runningCount > 0) "Running ($runningCount/5)" else "Stopped"
            _isServiceRunning.value = runningCount > 0
        }
    }

    private fun updateDebugText(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLine = "[$timestamp] $message"
        val currentText = _debugText.value
        val updatedText = if (currentText.isEmpty()) {
            newLine
        } else {
            currentText + "\n" + newLine
        }

        // Keep only last 5 lines
        val lines = updatedText.split("\n")
        val limitedText = if (lines.size > 5) {
            lines.takeLast(5).joinToString("\n")
        } else {
            updatedText
        }

        _debugText.value = limitedText
    }
}
