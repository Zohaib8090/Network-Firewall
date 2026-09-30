package dev.zohaib.networkfirewall.vpn

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

enum class ForegroundEventType { RESUMED, PAUSED }

data class ForegroundEvent(val timeMs: Long, val packageName: String, val type: ForegroundEventType)

/** Works out which app is in front from app-usage events. Pure, so it can be unit-tested. */
object ForegroundLogic {

    /**
     * Applies [events] to the previously known front app and returns the new one.
     * An app going to the background only clears the answer if it was the one in front, and the
     * launcher counts as an app, so closing an app to the home screen makes the launcher the answer.
     * Applying the same events twice gives the same result, so polls can safely overlap.
     */
    fun reduce(previous: String?, events: List<ForegroundEvent>): String? {
        var current = previous
        for (event in events.sortedBy { it.timeMs }) {
            when (event.type) {
                ForegroundEventType.RESUMED -> current = event.packageName
                ForegroundEventType.PAUSED -> if (current == event.packageName) current = null
            }
        }
        return current
    }
}

/** Polls [UsageStatsManager] for foreground changes. Needs Usage access; without it the answer is unknown. */
class ForegroundTracker(private val usageStats: UsageStatsManager?) {
    private var lastQueryEnd = 0L

    /** The app in front as of the last [poll], or null when unknown (screen off, no access). */
    var current: String? = null
        private set

    fun poll(now: Long = System.currentTimeMillis()): String? {
        val manager = usageStats ?: return current
        val from = if (lastQueryEnd == 0L) now - INITIAL_LOOKBACK_MS else lastQueryEnd - OVERLAP_MS
        val events = mutableListOf<ForegroundEvent>()
        try {
            val stream = manager.queryEvents(from, now)
            val event = UsageEvents.Event()
            while (stream.hasNextEvent()) {
                stream.getNextEvent(event)
                @Suppress("DEPRECATION") // same values as ACTIVITY_RESUMED / ACTIVITY_PAUSED, but works from API 24
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND ->
                        events.add(ForegroundEvent(event.timeStamp, event.packageName, ForegroundEventType.RESUMED))
                    UsageEvents.Event.MOVE_TO_BACKGROUND ->
                        events.add(ForegroundEvent(event.timeStamp, event.packageName, ForegroundEventType.PAUSED))
                }
            }
        } catch (e: SecurityException) {
            return current // Usage access was taken away
        }
        lastQueryEnd = now
        current = ForegroundLogic.reduce(current, events)
        return current
    }

    private companion object {
        const val INITIAL_LOOKBACK_MS = 6 * 60 * 60 * 1000L
        const val OVERLAP_MS = 1_000L
    }
}
