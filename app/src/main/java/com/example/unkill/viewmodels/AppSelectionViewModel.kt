package com.example.unkill.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unkill.models.AppInfo
import com.example.unkill.utils.AppUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AppSelectionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AppSelectionViewModel"
    }

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps

    // Additional tracking for selected apps to avoid StateFlow issues
    private val _selectedAppsInternal = MutableStateFlow<List<AppInfo>>(emptyList())
    val selectedAppsInternal: StateFlow<List<AppInfo>> = _selectedAppsInternal

    private val context = application

    init {
        // Set up reactive filtering of apps based on search query
        viewModelScope.launch {
            combine(_apps, _searchQuery) { apps, query ->
                if (query.isEmpty()) {
                    apps.sortedBy { it.appName.lowercase() }
                } else {
                    apps.filter {
                        it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                    }.sortedBy { it.appName.lowercase() }
                }
            }.collect { filtered ->
                _filteredApps.value = filtered
            }
        }
    }

    fun searchApps(query: String) {
        _searchQuery.value = query
        appendToDebugLog("Search query updated: '$query'")
    }

    fun loadInstalledApps() {
        _isLoading.value = true
        _errorMessage.value = ""
        appendToDebugLog("Loading installed apps...")

        viewModelScope.launch {
            try {
                val installedApps = AppUtils.getInstalledApps(context)
                
                // Load previously protected apps from preferences to set their isProtected status
                val sharedPreferences = context.getSharedPreferences("unkill_prefs", android.content.Context.MODE_PRIVATE)
                val protectedPackages = sharedPreferences.getStringSet("protected_apps", emptySet())?.toSet() ?: emptySet()
                
                // Update the apps list to reflect which ones are already protected
                val appsWithProtectionStatus = installedApps.map { app ->
                    app.copy(isProtected = app.packageName in protectedPackages)
                }
                
                _apps.value = appsWithProtectionStatus

                appendToDebugLog("Loaded ${installedApps.size} apps successfully, ${protectedPackages.size} previously protected")
                android.util.Log.d("AppSelectionViewModel", "Loaded ${installedApps.size} apps successfully, ${protectedPackages.size} previously protected")
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
        android.util.Log.d(TAG, "Toggling app ${appInfo.appName} [${appInfo.packageName}] to selected=$isSelected")

        // Update the apps list directly
        val currentApps = _apps.value
        val updatedApps = currentApps.map { app ->
            if (app.packageName == appInfo.packageName) {
                val oldIsProtected = app.isProtected
                val updatedApp = app.copy(isProtected = isSelected)
                android.util.Log.d(TAG, "Updated app [${app.packageName}] protected state: $oldIsProtected -> $isSelected")
                updatedApp
            } else {
                app
            }
        }

        // Set the new list
        _apps.value = updatedApps

        // Also update the internal selected apps tracking for reliability
        val currentSelected = _selectedAppsInternal.value
        val updatedSelected = if (isSelected) {
            // Add to selected list if not already present
            if (currentSelected.none { it.packageName == appInfo.packageName }) {
                currentSelected + appInfo.copy(isProtected = true)
            } else {
                currentSelected.map { if (it.packageName == appInfo.packageName) it.copy(isProtected = true) else it }
            }
        } else {
            // Remove from selected list
            currentSelected.filterNot { it.packageName == appInfo.packageName }
        }
        _selectedAppsInternal.value = updatedSelected

        // Verify the update was successful
        val verifyApp = updatedApps.find { it.packageName == appInfo.packageName }
        appendToDebugLog("Verification - App ${verifyApp?.appName} [${verifyApp?.packageName}] isProtected=${verifyApp?.isProtected}")

        // Update selected count immediately using the updated list
        updateSelectedCountFromList(updatedApps)
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
        // Force a synchronous read of the current state to avoid race conditions
        val currentApps = _apps.value
        val currentFilteredApps = _filteredApps.value
        val internalSelectedApps = _selectedAppsInternal.value

        // Use the main apps list to ensure we get the current selection state
        val selectedApps = currentApps.filter { it.isProtected }

        appendToDebugLog("Checking for selected apps - total apps: ${currentApps.size}, protected apps: ${selectedApps.size}")
        selectedApps.forEach { app ->
            appendToDebugLog("Found protected app: ${app.appName} [${app.packageName}]")
        }

        // Also check filtered apps to see if there's a state mismatch
        val filteredProtectedApps = currentFilteredApps.filter { it.isProtected }
        appendToDebugLog("Filtered apps check - total filtered: ${currentFilteredApps.size}, protected in filtered: ${filteredProtectedApps.size}")

        // Check internal selected apps tracking
        appendToDebugLog("Internal selected apps check - total internal: ${internalSelectedApps.size}")
        internalSelectedApps.forEach { app ->
            appendToDebugLog("Found internal selected app: ${app.appName} [${app.packageName}]")
        }

        // Additional debugging - check if the issue is with StateFlow timing
        if (selectedApps.isEmpty() && _selectedCount.value > 0) {
            appendToDebugLog("WARNING: State mismatch detected! Selected count shows ${_selectedCount.value} but no protected apps found")
            android.util.Log.w("AppSelectionViewModel", "State mismatch: count shows ${_selectedCount.value} but no protected apps")

            // Try the internal selected apps first
            if (internalSelectedApps.isNotEmpty()) {
                appendToDebugLog("Using internal selected apps as fallback - found ${internalSelectedApps.size} apps")
                protectAppsInternal(internalSelectedApps)
                return
            }

            // Try to recover by checking again after a brief delay
            kotlinx.coroutines.GlobalScope.launch {
                kotlinx.coroutines.delay(50) // Increased delay
                val retryApps = _apps.value
                val retrySelectedApps = retryApps.filter { it.isProtected }
                appendToDebugLog("Retry check - total apps: ${retryApps.size}, protected apps: ${retrySelectedApps.size}")

                if (retrySelectedApps.isNotEmpty()) {
                    appendToDebugLog("Recovery successful - found protected apps on retry")
                    // Continue with protection using the retry results
                    protectAppsInternal(retrySelectedApps)
                } else {
                    // Last resort: try to reconstruct the selected apps from the count
                    appendToDebugLog("CRITICAL: Still no apps found, attempting reconstruction from filtered apps")
                    val reconstructedApps = currentFilteredApps.filter { it.isProtected }
                    if (reconstructedApps.isNotEmpty()) {
                        appendToDebugLog("Reconstruction successful - found ${reconstructedApps.size} apps")
                        protectAppsInternal(reconstructedApps)
                    } else {
                        val errorMsg = "No apps selected (confirmed after retry and reconstruction)"
                        _errorMessage.value = errorMsg
                        appendToDebugLog("ERROR: $errorMsg")
                        android.util.Log.w("AppSelectionViewModel", errorMsg)
                    }
                }
            }
            return
        }

        if (selectedApps.isEmpty()) {
            val errorMsg = "No apps selected"
            _errorMessage.value = errorMsg
            appendToDebugLog("ERROR: $errorMsg")
            android.util.Log.w("AppSelectionViewModel", errorMsg)
            return
        }

        // If we get here, proceed with normal protection
        protectAppsInternal(selectedApps)
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

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    private fun logSelectedAppsToGlobalDebug(selectedApps: List<AppInfo>) {
        try {
            val sharedPreferences = context.getSharedPreferences("unkill_global_debug", android.content.Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            // Create a detailed log entry for the selected apps
            val logEntry = buildString {
                append("[$timestamp] APP SELECTION: Protected ${selectedApps.size} apps:\n")
                selectedApps.forEach { app ->
                    append("[$timestamp]   ✓ ${app.appName} (${app.packageName})\n")
                }
                append("[$timestamp] APP SELECTION: Protection service started\n")
            }

            // Get existing global debug log
            val existingLog = sharedPreferences.getString("global_debug_log", "")
            val updatedLog = if (existingLog.isNullOrEmpty()) {
                logEntry
            } else {
                existingLog + logEntry
            }

            // Keep only last 20 entries to prevent unlimited growth
            val lines = updatedLog.split("\n")
            val limitedLog = if (lines.size > 20) {
                lines.takeLast(20).joinToString("\n")
            } else {
                updatedLog
            }

            editor.putString("global_debug_log", limitedLog)
            editor.apply()

            appendToDebugLog("Logged ${selectedApps.size} selected apps to global debug log")
            android.util.Log.d("AppSelectionViewModel", "Logged ${selectedApps.size} apps to global debug")

        } catch (e: Exception) {
            appendToDebugLog("ERROR: Failed to log selected apps to global debug: ${e.message}")
            android.util.Log.e("AppSelectionViewModel", "Failed to log to global debug", e)
        }
    }

    private fun updateSelectedCount() {
        // Force refresh filtered apps first to ensure UI updates
        val currentApps = _apps.value
        val filteredApps = if (_searchQuery.value.isEmpty()) {
            currentApps.sortedBy { it.appName.lowercase() }
        } else {
            currentApps.filter {
                it.appName.contains(_searchQuery.value, ignoreCase = true) ||
                it.packageName.contains(_searchQuery.value, ignoreCase = true)
            }.sortedBy { it.appName.lowercase() }
        }
        _filteredApps.value = filteredApps

        // Now calculate the protected count from the filtered apps that are visible to the user
        val protectedCount = filteredApps.count { it.isProtected }

        android.util.Log.d(TAG, "Apps list size: ${currentApps.size}, filtered apps: ${filteredApps.size}, protected apps: $protectedCount")
        filteredApps.filter { it.isProtected }.forEach { app ->
            android.util.Log.d(TAG, "Protected app: ${app.appName} [${app.packageName}]")
        }

        android.util.Log.d(TAG, "Updating selected count: $protectedCount apps protected")
        _selectedCount.value = protectedCount
        appendToDebugLog("Selected count updated: $protectedCount")
    }

    private fun updateSelectedCountFromList(appsList: List<AppInfo>) {
        // Calculate filtered apps from the provided list
        val filteredApps = if (_searchQuery.value.isEmpty()) {
            appsList.sortedBy { it.appName.lowercase() }
        } else {
            appsList.filter {
                it.appName.contains(_searchQuery.value, ignoreCase = true) ||
                it.packageName.contains(_searchQuery.value, ignoreCase = true)
            }.sortedBy { it.appName.lowercase() }
        }
        _filteredApps.value = filteredApps

        // Calculate the protected count from the filtered apps
        val protectedCount = filteredApps.count { it.isProtected }

        android.util.Log.d(TAG, "Direct update - Apps list size: ${appsList.size}, filtered apps: ${filteredApps.size}, protected apps: $protectedCount")
        filteredApps.filter { it.isProtected }.forEach { app ->
            android.util.Log.d(TAG, "Protected app: ${app.appName} [${app.packageName}]")
        }

        android.util.Log.d(TAG, "Direct update - selected count: $protectedCount apps protected")
        _selectedCount.value = protectedCount
        appendToDebugLog("Selected count updated: $protectedCount")
    }

    private fun protectAppsInternal(selectedApps: List<AppInfo>) {
        appendToDebugLog("Protecting ${selectedApps.size} selected apps (internal)...")
        android.util.Log.i("AppSelectionViewModel", "Protecting ${selectedApps.size} apps (internal)")

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

                // Log selected apps to global debug log for MainActivity
                logSelectedAppsToGlobalDebug(selectedApps)

                appendToDebugLog("Saved ${protectedPackages.size} protected apps to preferences")
                android.util.Log.d("AppSelectionViewModel", "Saved ${protectedPackages.size} protected apps")

                // Start the UnkillServiceManager to handle the protection
                val intent = Intent(context, com.example.unkill.services.UnkillServiceManager::class.java).apply {
                    action = com.example.unkill.services.UnkillServiceManager.ACTION_START_SERVICE
                }

                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    appendToDebugLog("Successfully started UnkillServiceManager")
                    android.util.Log.d("AppSelectionViewModel", "Successfully started UnkillServiceManager")
                } catch (e: Exception) {
                    appendToDebugLog("Failed to start service: ${e.message}")
                    android.util.Log.e("AppSelectionViewModel", "Error starting service", e)
                    _errorMessage.value = "Failed to start protection service: ${e.message}"
                }

                appendToDebugLog("Started service instances for protected apps")
                android.util.Log.i("AppSelectionViewModel", "Started services for protected apps")

                _protectionComplete.value = true
                val successMsg = "Protected ${selectedApps.size} apps successfully"
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
}
