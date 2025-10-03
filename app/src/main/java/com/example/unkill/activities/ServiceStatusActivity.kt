package com.example.unkill.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unkill.adapters.ServiceStatusAdapter
import com.example.unkill.databinding.ActivityServiceStatusBinding
import com.example.unkill.viewmodels.ServiceStatusViewModel
import kotlinx.coroutines.launch

class ServiceStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceStatusBinding
    private val viewModel: ServiceStatusViewModel by viewModels()
    private lateinit var adapter: ServiceStatusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        loadServiceStatuses()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Service Status"
    }

    private fun setupRecyclerView() {
        adapter = ServiceStatusAdapter()
        android.util.Log.d("ServiceStatusActivity", "Setting up RecyclerView with adapter")
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ServiceStatusActivity)
            adapter = this@ServiceStatusActivity.adapter
            android.util.Log.d("ServiceStatusActivity", "RecyclerView adapter set, item count: ${this@ServiceStatusActivity.adapter.itemCount}")
        }
    }

    private fun setupButtons() {
        binding.btnRestartAll.setOnClickListener {
            viewModel.restartAllServices()
        }

        binding.btnStopAll.setOnClickListener {
            viewModel.stopAllServices()
        }

        binding.btnRefresh.setOnClickListener {
            loadServiceStatuses()
        }
    }

    private fun observeViewModel() {
        // Observe service statuses
        lifecycleScope.launch {
            viewModel.serviceStatuses.collect { statuses ->
                android.util.Log.d("ServiceStatusActivity", "Received ${statuses.size} service statuses")
                statuses.forEach { status ->
                    android.util.Log.d("ServiceStatusActivity", "Service ${status.serviceId}: ${status.state}, Memory: ${status.formattedMemoryUsage}, Uptime: ${status.formattedUptime}")
                }
                adapter.updateStatuses(statuses)
            }
        }

        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        // Observe error messages
        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                if (message.isNotEmpty()) {
                    android.util.Log.e("ServiceStatusActivity", "Error: $message")
                }
            }
        }

        // Observe action completion
        lifecycleScope.launch {
            viewModel.actionComplete.collect { isComplete ->
                if (isComplete) {
                    loadServiceStatuses() // Refresh the list
                }
            }
        }
    }

    private fun loadServiceStatuses() {
        viewModel.loadServiceStatuses()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
