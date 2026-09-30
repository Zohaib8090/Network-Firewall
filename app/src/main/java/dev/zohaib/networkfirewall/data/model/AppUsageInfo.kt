package dev.zohaib.networkfirewall.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val icon: ImageBitmap? = null,
    val wifiBytesToday: Long = 0L,
    val mobileBytesToday: Long = 0L,
    val totalBytesToday: Long = 0L,
    val totalBytesMonth: Long = 0L,
    val currentRxSpeedBytesPerSec: Long = 0L,
    val currentTxSpeedBytesPerSec: Long = 0L,
    val rule: AppRule = AppRule(packageName = packageName, appName = appName, isSystemApp = isSystemApp)
) {
    // Formatting uses String.format, which is slow; build each text once per instance instead of on
    // every redraw while the list scrolls.
    val totalFormattedToday: String by lazy(LazyThreadSafetyMode.NONE) { formatBytes(totalBytesToday) }

    val wifiFormattedToday: String by lazy(LazyThreadSafetyMode.NONE) { formatBytes(wifiBytesToday) }

    val mobileFormattedToday: String by lazy(LazyThreadSafetyMode.NONE) { formatBytes(mobileBytesToday) }

    /** The usage line shown in each list row. */
    val usageSummaryToday: String by lazy(LazyThreadSafetyMode.NONE) {
        "Today: $totalFormattedToday (Wi-Fi: $wifiFormattedToday, Cell: $mobileFormattedToday)"
    }

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
