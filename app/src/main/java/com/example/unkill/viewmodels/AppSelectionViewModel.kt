package com.example.unkill.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unkill.models.AppInfo
import com.example.unkill.utils.AppUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _protectionComplete = MutableStateFlow(false)
    val protectionComplete: StateFlow<Boolean> = _protectionComplete

    private val _debugLog = MutableStateFlow("Debug log will appear here...\n")
    val debugLog: StateFlow<String> = _debugLog

    private val context = application

    fun loadInstalledApps() {
        _isLoading.value = true
        _errorMessage.value = ""
        appendToDebugLog("Loading installed apps...")

        viewModelScope.launch {
            try {
                val installedApps = AppUtils.getInstalledApps(context)
                _apps.value = installedApps
                appendToDebugLog("Loaded ${installedApps.size} apps successfully")
                android.util.Log.d("AppSelectionViewModel", "Loaded ${installedApps.size} apps successfully")
            } catch (e: Exception) {
                val errorMsg = "Failed to load apps: ${e.message}"
                _errorMessage.value = errorMsg
                appendToDebugLog("ERROR: $errorMsg")
                android.util.Log.e("AppSelectionViewModel", errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAppSelection(appInfo: AppInfo, isSelected: Boolean) {
        val currentApps = _apps.value.toMutableList()
        val index = currentApps.indexOfFirst { it.packageName == appInfo.packageName }

        if (index != -1) {
            currentApps[index] = currentApps[index].copy(isProtected = isSelected)
            _apps.value = currentApps

            // Update selected count
            updateSelectedCount()
        }
    }

    fun selectAllApps() {
        val currentApps = _apps.value.map { it.copy(isProtected = true) }
        _apps.value = currentApps
        updateSelectedCount()
    }

    fun deselectAllApps() {
        val currentApps = _apps.value.map { it.copy(isProtected = false) }
        _apps.value = currentApps
        updateSelectedCount()
    }

    fun protectSelectedApps() {
        val selectedApps = _apps.value.filter { it.isProtected }

        if (selectedApps.isEmpty()) {
            val errorMsg = "No apps selected"
            _errorMessage.value = errorMsg
            appendToDebugLog("ERROR: $errorMsg")
            android.util.Log.w("AppSelectionViewModel", errorMsg)
            return
        }

        appendToDebugLog("Protecting ${selectedApps.size} selected apps...")
        android.util.Log.i("AppSelectionViewModel", "Protecting ${selectedApps.size} apps")

        viewModelScope.launch {
            try {
                // Store selected apps in shared preferences
                val sharedPreferences = context.getSharedPreferences("unkill_prefs", android.content.Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                // Save the list of protected app package names
                val protectedPackages = selectedApps.map { it.packageName }
                editor.putStringSet("protected_apps", protectedPackages.toSet())
                editor.putInt("protected_apps_count", protectedPackages.size)
                editor.apply()

                appendToDebugLog("Saved ${protectedPackages.size} protected apps to preferences")
                android.util.Log.d("AppSelectionViewModel", "Saved ${protectedPackages.size} protected apps")

                // Start service instances to monitor these apps
                val serviceManager = com.example.unkill.services.UnkillServiceManager.getInstance(context)
                serviceManager.startServicesForApps(protectedPackages)

                appendToDebugLog("Started service instances for protected apps")
                android.util.Log.i("AppSelectionViewModel", "Started services for protected apps")

                _protectionComplete.value = true
                val successMsg = "Protected ${selectedApps.size} apps successfully"
                _errorMessage.value = successMsg
                appendToDebugLog("SUCCESS: $successMsg")
                android.util.Log.i("AppSelectionViewModel", successMsg)

            } catch (e: Exception) {
                val errorMsg = "Failed to protect apps: ${e.message}"
                _errorMessage.value = errorMsg
                appendToDebugLog("ERROR: $errorMsg")
                android.util.Log.e("AppSelectionViewModel", errorMsg, e)
            }
        }
    }

    fun appendToDebugLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message\n"
        _debugLog.value += logEntry
    }

    fun clearDebugLog() {
        _debugLog.value = "Debug log cleared\n"
        android.util.Log.d("AppSelectionViewModel", "Debug log cleared")
    }

    private fun updateSelectedCount() {
        _selectedCount.value = _apps.value.count { it.isProtected }
        appendToDebugLog("Selected count updated: ${_selectedCount.value}")
    }
}
