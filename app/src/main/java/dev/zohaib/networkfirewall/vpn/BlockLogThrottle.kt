package dev.zohaib.networkfirewall.vpn

/**
 * Keeps the block log from growing without bound. A blocked app retries constantly, so the same
 * attempt would otherwise be written over and over. Pure, so it can be unit-tested.
 *
 * - The app the user is looking at is logged once per destination every [foregroundWindowMs].
 * - Apps in the background are logged once per app every [backgroundWindowMs], whatever they
 *   contact: during Global Lock hundreds of apps knock in the background.
 */
class BlockLogThrottle(
    private val foregroundWindowMs: Long = 10_000L,
    private val backgroundWindowMs: Long = 60_000L,
    private val maxEntries: Int = 1_000
) {
    private val lastLogged = HashMap<String, Long>()

    @Synchronized
    fun shouldLog(packageName: String, destIp: String, destPort: Int, isForeground: Boolean, now: Long): Boolean {
        val key = if (isForeground) "$packageName|$destIp|$destPort" else packageName
        val window = if (isForeground) foregroundWindowMs else backgroundWindowMs
        val previous = lastLogged[key]
        if (previous != null && now - previous < window) return false

        lastLogged[key] = now
        if (lastLogged.size > maxEntries) {
            val oldestUseful = maxOf(foregroundWindowMs, backgroundWindowMs)
            lastLogged.entries.removeAll { now - it.value >= oldestUseful }
        }
        return true
    }
}
