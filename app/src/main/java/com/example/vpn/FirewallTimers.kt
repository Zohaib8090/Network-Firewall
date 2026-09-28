package com.example.vpn

import com.example.data.model.AppRule
import com.example.data.model.ScheduleRule
import java.util.Calendar
import java.util.TimeZone

object FirewallTimers {

    /**
     * Returns the next moment the set of blocked apps may change on its own:
     * a pause ending, a temporary allowance expiring, or a schedule starting or ending.
     * Returns null when nothing time-based is pending.
     */
    fun nextChangeAt(
        now: Long,
        pausedUntil: Long,
        rules: List<AppRule>,
        schedules: List<ScheduleRule>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        val candidates = mutableListOf<Long>()
        if (pausedUntil > now) candidates.add(pausedUntil)

        for (rule in rules) {
            if (rule.temporaryAccessUntil > now) candidates.add(rule.temporaryAccessUntil)
        }

        for (schedule in schedules) {
            if (!schedule.isEnabled) continue
            candidates.add(nextOccurrence(now, schedule.startHour, schedule.startMinute, timeZone))
            // ScheduleRule.isActiveAt treats the end minute as inclusive, so blocking lifts a minute later
            val endExclusive = (schedule.endHour * 60 + schedule.endMinute + 1) % MINUTES_PER_DAY
            candidates.add(nextOccurrence(now, endExclusive / 60, endExclusive % 60, timeZone))
        }

        return candidates.minOrNull()
    }

    fun nextOccurrence(now: Long, hour: Int, minute: Int, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
