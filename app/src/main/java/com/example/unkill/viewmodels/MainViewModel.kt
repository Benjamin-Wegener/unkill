package com.example.unkill.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
                    val intent = Intent(context, UnkillServiceManager::class.java).apply {
                        action = UnkillServiceManager.ACTION_START_SERVICE
                        putExtra(UnkillServiceManager.EXTRA_SERVICE_ID, i)
                    }
                    context.startService(intent)
                }
                _serviceStatus.value = "Running"
                _activeInstances.value = 5
                _errorMessage.value = "All services started successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start services: ${e.message}"
            }
        }
    }

    fun stopAllServices() {
        viewModelScope.launch {
            try {
                for (i in 1..5) {
                    val intent = Intent(context, UnkillServiceManager::class.java).apply {
                        action = UnkillServiceManager.ACTION_STOP_SERVICE
                        putExtra(UnkillServiceManager.EXTRA_SERVICE_ID, i)
                    }
                    context.stopService(intent)
                }
                _serviceStatus.value = "Stopped"
                _activeInstances.value = 0
                _errorMessage.value = "All services stopped"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop services: ${e.message}"
            }
        }
    }

    fun refreshServiceStatus() {
        viewModelScope.launch {
            val runningCount = (1..5).count { serviceId ->
                UnkillServiceManager.isServiceRunning(serviceId)
            }
            _activeInstances.value = runningCount
            _serviceStatus.value = if (runningCount > 0) "Running ($runningCount/5)" else "Stopped"
        }
    }

    fun showPermissionToast(context: Context) {
        viewModelScope.launch {
            val missingPermissions = PermissionUtils.getMissingPermissions(context)
            if (missingPermissions.isNotEmpty()) {
                android.util.Log.w("Unkill", "Missing permissions: ${missingPermissions.joinToString(", ")}")
            }
        }
    }
}
