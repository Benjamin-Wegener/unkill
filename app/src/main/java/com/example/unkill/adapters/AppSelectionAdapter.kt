package com.example.unkill.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unkill.databinding.ItemAppSelectionBinding
import com.example.unkill.models.AppInfo

class AppSelectionAdapter(
    private val onSelectionChanged: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>() {

    private var apps: List<AppInfo> = emptyList()

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(private val binding: ItemAppSelectionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo) {
            binding.apply {
                ivAppIcon.setImageDrawable(appInfo.icon)
                tvAppName.text = appInfo.appName
                tvPackageName.text = appInfo.packageName

                // Show system app indicator
                ivSystemApp.visibility = if (appInfo.isSystemApp) View.VISIBLE else View.GONE

                // Set selection state
                checkbox.isChecked = appInfo.isProtected

                // Handle selection changes
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionChanged(appInfo, isChecked)
                }

                // Handle item clicks
                container.setOnClickListener {
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }
    }
}
