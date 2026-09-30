package com.example.vpn

import com.example.data.model.AppRule
import com.example.data.model.ScheduleRule

/** Decides which apps the firewall blocks right now. Pure, so it can be unit-tested. */
object BlockPlanner {

    /**
     * @param rules one rule per known app
     * @param installedPackages every installed package, used by Global Lock
     * @param hour, minute, dayCode the current local time (dayCode is MON..SUN)
     * @return package names to route into the tunnel and drop
     *
     * An app the user allowed for a while ([AppRule.isTemporarilyAllowed]) is never blocked, whatever
     * the reason: its own toggles, a schedule or Global Lock.
     */
    fun blockedPackages(
        rules: List<AppRule>,
        installedPackages: Collection<String>,
        schedules: List<ScheduleRule>,
        isGlobalLock: Boolean,
        isWifi: Boolean,
        ownPackage: String,
        now: Long,
        hour: Int,
        minute: Int,
        dayCode: String
    ): Set<String> {
        val allowedNow = rules.filter { it.isTemporarilyAllowed(now) }.map { it.packageName }.toSet()

        if (isGlobalLock) {
            return installedPackages.filter { it != ownPackage && it !in allowedNow }.toSet()
        }

        val scheduled = scheduledPackages(rules, schedules, isWifi, hour, minute, dayCode)
        val blocked = mutableSetOf<String>()
        for (rule in rules) {
            if (rule.packageName == ownPackage || rule.packageName in allowedNow) continue
            if (rule.isEffectivelyBlocked(isWifi, now) || rule.packageName in scheduled) {
                blocked.add(rule.packageName)
            }
        }
        return blocked
    }

    private fun scheduledPackages(
        rules: List<AppRule>,
        schedules: List<ScheduleRule>,
        isWifi: Boolean,
        hour: Int,
        minute: Int,
        dayCode: String
    ): Set<String> {
        val result = mutableSetOf<String>()
        for (schedule in schedules) {
            if (!schedule.isActiveAt(hour, minute, dayCode)) continue
            if (!(if (isWifi) schedule.blockWifi else schedule.blockMobile)) continue
            if (schedule.targetPackageNames.trim() == "ALL") {
                rules.forEach { result.add(it.packageName) }
            } else {
                schedule.targetPackageNames.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { result.add(it) }
            }
        }
        return result
    }
}
