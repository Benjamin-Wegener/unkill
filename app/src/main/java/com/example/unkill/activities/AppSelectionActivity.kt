package com.example.unkill.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unkill.adapters.AppSelectionAdapter
import com.example.unkill.databinding.ActivityAppSelectionBinding
import com.example.unkill.viewmodels.AppSelectionViewModel
import kotlinx.coroutines.launch

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private val viewModel: AppSelectionViewModel by viewModels()
    private lateinit var adapter: AppSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        loadApps()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Apps to Protect"
    }

    private fun setupRecyclerView() {
        adapter = AppSelectionAdapter { appInfo, isSelected ->
            viewModel.toggleAppSelection(appInfo, isSelected)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AppSelectionActivity)
            adapter = this@AppSelectionActivity.adapter
        }
    }

    private fun setupButtons() {
        binding.btnProtectSelected.setOnClickListener {
            viewModel.protectSelectedApps()
        }

        binding.btnSelectAll.setOnClickListener {
            viewModel.selectAllApps()
        }

        binding.btnDeselectAll.setOnClickListener {
            viewModel.deselectAllApps()
        }

        binding.btnClearLog.setOnClickListener {
            viewModel.clearDebugLog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.apps.collect { apps ->
                adapter.updateApps(apps)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCount.collect { count ->
                binding.tvSelectedCount.text = "Selected: $count apps"
                binding.btnProtectSelected.isEnabled = count > 0
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                if (message.isNotEmpty()) {
                    // Error messages are now logged to debug log instead of showing toasts
                    android.util.Log.w("AppSelectionActivity", "Error: $message")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.protectionComplete.collect { isComplete ->
                if (isComplete) {
                    // Success message is now logged to debug log instead of showing toast
                    android.util.Log.i("AppSelectionActivity", "Protection complete - finishing activity")
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.debugLog.collect { logContent ->
                binding.tvDebugLog.text = logContent
                // Auto-scroll to bottom
                binding.tvDebugLog.post {
                    val scrollView = binding.tvDebugLog.parent as? android.widget.ScrollView
                    scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun loadApps() {
        viewModel.loadInstalledApps()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
