package com.example.unkill.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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

        setupUI()
        observeViewModel()
        checkPermissions()
    }

    private fun setupUI() {
        binding.btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        binding.btnViewStatus.setOnClickListener {
            startActivity(Intent(this, ServiceStatusActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnInstallStandalone.setOnClickListener {
            installStandaloneServices()
        }

        binding.btnStartServices.setOnClickListener {
            viewModel.startAllServices()
        }

        binding.btnStopServices.setOnClickListener {
            viewModel.stopAllServices()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.serviceStatus.collect { status ->
                binding.tvServiceStatus.text = getString(R.string.service_status, status)
            }
        }

        lifecycleScope.launch {
            viewModel.activeInstances.collect { count ->
                binding.tvActiveInstances.text = getString(R.string.active_instances, count)
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                if (message.isNotEmpty()) {
                    // Log to LogCat as requested
                    android.util.Log.e("Unkill", "ERROR: $message")
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPermissions() {
        viewModel.checkRequiredPermissions()
        viewModel.showPermissionToast(this)
    }

    private fun installStandaloneServices() {
        Toast.makeText(this, "Building and installing service APKs...", Toast.LENGTH_SHORT).show()

        val apkInstaller = ApkInstaller(this)
        val serviceModules = listOf("service1", "service2", "service3", "service4", "service5")
        val apkPaths = mutableListOf<String>()

        lifecycleScope.launch {
            try {
                // Build all service APKs
                for (module in serviceModules) {
                    apkInstaller.buildServiceApk(module) { success, apkPath ->
                        if (success && apkPath != null) {
                            apkPaths.add(apkPath)
                        }
                    }
                }

                // Install all built APKs
                if (apkPaths.size == serviceModules.size) {
                    apkInstaller.installApks(apkPaths) { success, message ->
                        Toast.makeText(
                            this@MainActivity,
                            if (success) "Successfully installed ${apkPaths.size} service APKs"
                            else "Failed to install service APKs: $message",
                            Toast.LENGTH_LONG
                        ).show()

                        if (success) {
                            // Start all services in separate processes
                            startStandaloneServices()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to build all service APKs (${apkPaths.size}/${serviceModules.size} built)",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error installing services: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startStandaloneServices() {
        Toast.makeText(this, "Starting standalone services...", Toast.LENGTH_SHORT).show()

        // In a real implementation, this would start intents for the separate service APKs
        // For demonstration, we'll show that we would start separate processes

        val servicePackages = listOf(
            "com.example.unkill.service1",
            "com.example.unkill.service2",
            "com.example.unkill.service3",
            "com.example.unkill.service4",
            "com.example.unkill.service5"
        )

        for (packageName in servicePackages) {
            try {
                // In production, start service in separate APK by launching its service activity/command
                val intent = Intent().apply {
                    setPackage(packageName)
                    action = "com.example.unkill.START_SERVICE"
                    // This would be a broadcast/intent that the service APK responds to
                }
                // sendBroadcast(intent) // For cross-app service starting

                android.util.Log.d("MainActivity", "Would start service in package: $packageName")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error starting service $packageName", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
    }
}
