package com.example.data.model

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val icon: Drawable? = null,
    val wifiBytesToday: Long = 0L,
    val mobileBytesToday: Long = 0L,
    val totalBytesToday: Long = 0L,
    val totalBytesMonth: Long = 0L,
    val currentRxSpeedBytesPerSec: Long = 0L,
    val currentTxSpeedBytesPerSec: Long = 0L,
    val rule: AppRule = AppRule(packageName = packageName, appName = appName, isSystemApp = isSystemApp)
) {
    val totalFormattedToday: String
        get() = formatBytes(totalBytesToday)

    val wifiFormattedToday: String
        get() = formatBytes(wifiBytesToday)

    val mobileFormattedToday: String
        get() = formatBytes(mobileBytesToday)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
