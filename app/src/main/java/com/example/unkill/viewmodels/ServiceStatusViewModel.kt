package com.example.unkill.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unkill.models.ServiceStatus
import com.example.unkill.services.UnkillServiceManager
import kotlinx.coroutines.delay
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

    private var isObserverStarted = false

    fun loadServiceStatuses() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                // Ensure service manager is initialized
                val context = getApplication<Application>().applicationContext
                UnkillServiceManager.getInstance(context)

                val statuses = UnkillServiceManager.getAllServiceStatuses()
                val sortedStatuses = statuses.values.toList().sortedBy { it.serviceId }
                val newList = sortedStatuses.toList() // Force new list creation

                _serviceStatuses.value = newList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load service statuses: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

        // Start observing for status updates (only once)
        if (!isObserverStarted) {
            startStatusObserver()
            isObserverStarted = true
        }
    }

    private fun startStatusObserver() {
        viewModelScope.launch {
            try {
                while (true) {
                    delay(2000) // Update every 2 seconds
                    val statuses = UnkillServiceManager.getAllServiceStatuses()
                    val sortedStatuses = statuses.values.toList().sortedBy { it.serviceId }
                    val newList = sortedStatuses.toList() // Force new list creation

                    _serviceStatuses.value = newList
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _errorMessage.value = "Failed to update service statuses: ${e.message}"
                }
                // The coroutine will naturally end when cancelled
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
