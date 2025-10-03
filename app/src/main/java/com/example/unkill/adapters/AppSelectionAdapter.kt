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
        android.util.Log.d("AppSelectionAdapter", "Updated adapter with ${newApps.size} apps, ${newApps.count { it.isProtected }} selected")
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

                android.util.Log.d("AppSelectionAdapter", "Binding app ${appInfo.appName} [${appInfo.packageName}], isProtected=${appInfo.isProtected}")

                // Handle selection changes
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    android.util.Log.d("AppSelectionAdapter", "Checkbox changed for ${appInfo.appName} to $isChecked")
                    onSelectionChanged(appInfo, isChecked)
                }

                // Set selection state (after setting listener to avoid triggering)
                checkbox.isChecked = appInfo.isProtected
                android.util.Log.d("AppSelectionAdapter", "Set checkbox for ${appInfo.appName} to ${appInfo.isProtected}")

                // Handle item clicks
                container.setOnClickListener {
                    val newState = !checkbox.isChecked
                    android.util.Log.d("AppSelectionAdapter", "Container clicked for ${appInfo.appName}, toggling to $newState")
                    checkbox.isChecked = newState
                }
            }
        }
    }
}
