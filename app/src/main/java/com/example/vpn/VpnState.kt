package com.example.vpn

enum class VpnStatus {
    STOPPED,
    STARTING,
    ACTIVE,
    PAUSED,
    GLOBAL_LOCKED
}

data class BlockedAttemptEvent(
    val packageName: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val networkType: String, // "WIFI" or "CELLULAR"
    val destIp: String = "",
    val destPort: Int = 0
)
