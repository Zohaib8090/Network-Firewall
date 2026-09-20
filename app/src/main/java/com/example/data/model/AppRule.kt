package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isWifiBlocked: Boolean = false,
    val isMobileBlocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val dailyLimitBytes: Long = 0L,
    val weeklyLimitBytes: Long = 0L,
    val monthlyLimitBytes: Long = 0L,
    val temporaryAccessUntil: Long = 0L, // Timestamp in epoch ms
    val allowSession: Boolean = false,
    val notes: String = "",
    val isPinned: Boolean = false
) {
    fun isTemporarilyAllowed(currentTime: Long = System.currentTimeMillis()): Boolean {
        return allowSession || (temporaryAccessUntil > currentTime)
    }

    fun isEffectivelyBlocked(isWifiNetwork: Boolean, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (isTemporarilyAllowed(currentTime)) return false
        return if (isWifiNetwork) isWifiBlocked else isMobileBlocked
    }
}
