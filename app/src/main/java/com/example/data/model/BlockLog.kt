package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_logs")
data class BlockLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val networkType: String, // "WIFI" or "CELLULAR"
    val reason: String, // "Manual Block", "Scheduled Policy", "Limit Exceeded", "Global Lock"
    val ipAddress: String = "",
    val port: Int = 0
)
