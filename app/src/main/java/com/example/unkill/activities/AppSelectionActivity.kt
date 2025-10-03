package com.example.unkill.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
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
        setupSearchInput()
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

    private fun setupSearchInput() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                viewModel.searchApps(s?.toString() ?: "")
            }
        })
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
            android.util.Log.d("AppSelectionActivity", "Protect selected button clicked")
            // Add debugging to see current state before calling protectSelectedApps
            android.util.Log.d("AppSelectionActivity", "Current selected count: ${viewModel.selectedCount.value}")
            viewModel.protectSelectedApps()
        }

        binding.btnSelectAll.setOnClickListener {
            viewModel.selectAllApps()
        }

        binding.btnDeselectAll.setOnClickListener {
            viewModel.deselectAllApps()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.filteredApps.collect { filteredApps ->
                adapter.updateApps(filteredApps)
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
                    android.util.Log.w("AppSelectionActivity", "Error: $message")
                    viewModel.clearErrorMessage()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.protectionComplete.collect { isComplete ->
                if (isComplete) {
                    android.util.Log.i("AppSelectionActivity", "Protection complete - finishing activity")
                    finish()
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
