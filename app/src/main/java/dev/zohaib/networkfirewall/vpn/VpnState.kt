package dev.zohaib.networkfirewall.vpn

enum class VpnStatus {
    STOPPED,
    STARTING,
    ACTIVE,
    PAUSED,
    GLOBAL_LOCKED
}

/** Why the firewall turned itself off, shown to the user so a stopped firewall is never mistaken for an active one. */
enum class VpnStopReason(val message: String) {
    REVOKED("Firewall stopped: another VPN app took over. Turn the firewall on again to resume blocking."),
    NOT_PERMITTED("Firewall couldn't start: VPN permission is missing. Turn it on again and allow the VPN request."),
    FAILED("Firewall couldn't start. Turn it on again to retry.")
}

data class BlockedAttemptEvent(
    val packageName: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val networkType: String, // "WIFI" or "CELLULAR"
    val destIp: String = "",
    val destPort: Int = 0
)
