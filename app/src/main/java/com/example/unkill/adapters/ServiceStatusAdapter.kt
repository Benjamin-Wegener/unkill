package com.example.unkill.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.unkill.databinding.ItemServiceStatusBinding
import com.example.unkill.models.ServiceStatus
import com.example.unkill.models.ServiceState

class ServiceStatusAdapter : RecyclerView.Adapter<ServiceStatusAdapter.ServiceViewHolder>() {

    private var serviceStatuses: List<ServiceStatus> = emptyList()

    fun updateStatuses(newStatuses: List<ServiceStatus>) {
        serviceStatuses = newStatuses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceStatusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(serviceStatuses[position])
    }

    override fun getItemCount(): Int = serviceStatuses.size

    inner class ServiceViewHolder(private val binding: ItemServiceStatusBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(serviceStatus: ServiceStatus) {
            binding.apply {
                tvServiceName.text = "Service ${serviceStatus.serviceId}"
                tvStatus.text = serviceStatus.state.name
                tvMemoryUsage.text = serviceStatus.formattedMemoryUsage
                tvUptime.text = serviceStatus.formattedUptime

                // Set status color
                val statusColor = when (serviceStatus.state) {
                    ServiceState.RUNNING -> android.R.color.holo_green_dark
                    ServiceState.STOPPED -> android.R.color.holo_red_dark
                    ServiceState.RESTARTING -> android.R.color.holo_orange_dark
                    ServiceState.IDLE -> android.R.color.darker_gray
                }

                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))

                // Show/hide monitoring indicator
                ivMonitoring.visibility = if (serviceStatus.isMonitoring)
                    View.VISIBLE else View.GONE

                // Set protected apps count
                tvProtectedApps.text = "Protected apps: ${serviceStatus.protectedAppsCount}"
            }
        }
    }
}
