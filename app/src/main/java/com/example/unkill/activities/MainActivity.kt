package com.example.unkill.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unkill.R
import com.example.unkill.databinding.ActivityMainBinding
import com.example.unkill.utils.ApkInstaller
import com.example.unkill.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        observeViewModel()
        checkPermissions()
        checkAndInstallStandaloneServices()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Unkill"
        binding.toolbar.setLogo(R.mipmap.ic_launcher)
    }

    private fun setupUI() {
        binding.btnToggleServices.setOnClickListener {
            viewModel.toggleServices()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                if (message.isNotEmpty()) {
                    // Log to LogCat as requested
                    android.util.Log.e("Unkill", "ERROR: $message")
                    updateDebugText("ERROR: $message")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.debugText.collect { debugInfo ->
                // Merge with global debug log from app selection
                val globalDebugLog = getGlobalDebugLog()
                val combinedDebug = if (globalDebugLog.isNotEmpty()) {
                    globalDebugLog + "\n" + debugInfo
                } else {
                    debugInfo
                }
                binding.tvDebugScroll.text = combinedDebug
            }
        }

        lifecycleScope.launch {
            viewModel.isServiceRunning.collect { isRunning ->
                updateToggleButton(isRunning)
            }
        }
    }

    private fun checkPermissions() {
        viewModel.checkRequiredPermissions()

        // Automatically request missing permissions at startup for first-time users
        lifecycleScope.launch {
            val missingPermissions = com.example.unkill.utils.PermissionUtils.getMissingPermissions(this@MainActivity)
            if (missingPermissions.isNotEmpty()) {
                requestMissingPermissions(missingPermissions)
            }
        }
    }

    private fun requestMissingPermissions(missingPermissions: List<String>) {
        for (permission in missingPermissions) {
            when (permission) {
                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> {
                    com.example.unkill.utils.PermissionUtils.requestIgnoreBatteryOptimization(this)
                }
                "android.permission.PACKAGE_USAGE_STATS" -> {
                    com.example.unkill.utils.PermissionUtils.requestUsageStatsPermission(this)
                }
                // Runtime permissions are handled by the system automatically
                // when using Activity Result API or similar
            }
        }
    }

    private fun checkAndInstallStandaloneServices() {
        val apkInstaller = ApkInstaller(this)
        val serviceModules = listOf("service1", "service2", "service3", "service4", "service5")

        lifecycleScope.launch {
            try {
                // Check if services are already installed
                val allInstalled = serviceModules.all { module ->
                    apkInstaller.isServiceInstalled(module)
                }

                if (!allInstalled) {
                    updateDebugText("Installing standalone services...")
                    apkInstaller.buildAndInstallAllServices(serviceModules) { success, message ->
                        if (success) {
                            updateDebugText("All services installed successfully")
                        } else {
                            updateDebugText("Failed to install services: $message")
                        }
                    }
                } else {
                    updateDebugText("All services already installed")
                }
            } catch (e: Exception) {
                updateDebugText("Error checking/installing services: ${e.message}")
            }
        }
    }

    private fun updateDebugText(message: String) {
        val currentText = binding.tvDebugScroll.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLine = "[$timestamp] $message"
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

        binding.tvDebugScroll.text = limitedText
    }

    private fun updateToggleButton(isRunning: Boolean) {
        if (isRunning) {
            binding.btnToggleServices.text = "Stop Services"
            binding.btnToggleServices.icon = resources.getDrawable(android.R.drawable.ic_media_pause, null)
            binding.btnToggleServices.backgroundTintList = resources.getColorStateList(android.R.color.holo_red_dark, null)
        } else {
            binding.btnToggleServices.text = "Start Services"
            binding.btnToggleServices.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)
            binding.btnToggleServices.backgroundTintList = resources.getColorStateList(R.color.status_running, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_apps -> {
                startActivity(Intent(this, AppSelectionActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getGlobalDebugLog(): String {
        return try {
            val sharedPreferences = getSharedPreferences("unkill_global_debug", MODE_PRIVATE)
            sharedPreferences.getString("global_debug_log", "") ?: ""
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error reading global debug log", e)
            ""
        }
    }

    private fun clearGlobalDebugLog() {
        try {
            val sharedPreferences = getSharedPreferences("unkill_global_debug", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("global_debug_log", "")
            editor.apply()
            updateDebugText("Debug log cleared")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error clearing global debug log", e)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
        // Refresh debug text when returning to main activity to show any new global debug entries
        lifecycleScope.launch {
            val globalDebugLog = getGlobalDebugLog()
            if (globalDebugLog.isNotEmpty()) {
                val currentDebugText = viewModel.debugText.value
                if (currentDebugText.isNotEmpty()) {
                    binding.tvDebugScroll.text = globalDebugLog + "\n" + currentDebugText
                } else {
                    binding.tvDebugScroll.text = globalDebugLog
                }
            }
        }
    }
}
