package com.example.unkill.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unkill.models.ServiceStatus
import com.example.unkill.services.UnkillServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val _serviceStatuses = MutableStateFlow<List<ServiceStatus>>(emptyList())
    val serviceStatuses: StateFlow<List<ServiceStatus>> = _serviceStatuses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _actionComplete = MutableStateFlow(false)
    val actionComplete: StateFlow<Boolean> = _actionComplete

    fun loadServiceStatuses() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val statuses = UnkillServiceManager.getAllServiceStatuses()
                _serviceStatuses.value = statuses.values.toList().sortedBy { it.serviceId }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load service statuses: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restartAllServices() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                for (i in 1..5) {
                    val intent = android.content.Intent(
                        getApplication<Application>().applicationContext,
                        UnkillServiceManager::class.java
                    ).apply {
                        action = UnkillServiceManager.ACTION_RESTART_SERVICE
                        putExtra(UnkillServiceManager.EXTRA_SERVICE_ID, i)
                    }
                    getApplication<Application>().startService(intent)
                }
                _actionComplete.value = true
                _errorMessage.value = "All services restarted"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restart services: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopAllServices() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                for (i in 1..5) {
                    val intent = android.content.Intent(
                        getApplication<Application>().applicationContext,
                        UnkillServiceManager::class.java
                    ).apply {
                        action = UnkillServiceManager.ACTION_STOP_SERVICE
                        putExtra(UnkillServiceManager.EXTRA_SERVICE_ID, i)
                    }
                    getApplication<Application>().stopService(intent)
                }
                _actionComplete.value = true
                _errorMessage.value = "All services stopped"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop services: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
