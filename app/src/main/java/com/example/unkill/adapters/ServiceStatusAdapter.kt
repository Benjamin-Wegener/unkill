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
        if (serviceStatuses != newStatuses) {
            serviceStatuses = newStatuses
            notifyDataSetChanged()
        }
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

                // Set status color using app's defined colors instead of system colors
                val statusColorRes = when (serviceStatus.state) {
                    ServiceState.RUNNING -> com.example.unkill.R.color.status_running
                    ServiceState.STOPPED -> com.example.unkill.R.color.status_stopped
                    ServiceState.RESTARTING -> com.example.unkill.R.color.status_restarting
                    ServiceState.IDLE -> com.example.unkill.R.color.status_idle
                }

                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColorRes))

                // Ensure text is visible in both light and dark modes
                // Use a color that contrasts well with both light and dark backgrounds
                val visibleTextColor = ContextCompat.getColor(root.context, com.example.unkill.R.color.text_on_primary)
                tvServiceName.setTextColor(visibleTextColor)
                tvMemoryUsage.setTextColor(visibleTextColor)
                tvUptime.setTextColor(visibleTextColor)
                tvProtectedApps.setTextColor(visibleTextColor)

                // Show/hide monitoring indicator
                ivMonitoring.visibility = if (serviceStatus.isMonitoring)
                    View.VISIBLE else View.GONE

                // Set protected apps count
                tvProtectedApps.text = "Protected apps: ${serviceStatus.protectedAppsCount}"
            }
        }
    }
}
