package com.example.unkill.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isProtected: Boolean = false,
    val isSystemApp: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        return if (other is AppInfo) {
            packageName == other.packageName
        } else false
    }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }
}
