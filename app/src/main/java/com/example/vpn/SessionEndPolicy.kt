package com.example.vpn

/**
 * Decides when an "allow while the app is open" allowance should end. Pure, so it can be unit-tested.
 *
 * - Once the app has been seen in front and then leaves, the allowance ends after [graceMs]; the
 *   wait avoids re-blocking during a brief detour such as a permission dialog or the camera.
 * - If the app is never opened (the user allowed it from the list and didn't launch it), the
 *   allowance ends after [neverOpenedTimeoutMs] so it can't linger forever.
 * - While the app in front is unknown (screen off), nothing ends.
 */
class SessionEndPolicy(private val graceMs: Long, private val neverOpenedTimeoutMs: Long) {
    private val firstSeen = HashMap<String, Long>()
    private val opened = HashSet<String>()
    private val leftAt = HashMap<String, Long>()

    /** @return the allowances that should end now, out of the currently active [sessions] */
    fun update(sessions: Collection<String>, front: String?, now: Long): List<String> {
        val active = sessions.toSet()
        firstSeen.keys.retainAll(active)
        opened.retainAll(active)
        leftAt.keys.retainAll(active)

        val ended = mutableListOf<String>()
        for (pkg in active) {
            val seen = firstSeen.getOrPut(pkg) { now }
            when {
                pkg == front -> {
                    opened.add(pkg)
                    leftAt.remove(pkg)
                }
                pkg in opened -> {
                    if (front != null) {
                        val since = leftAt.getOrPut(pkg) { now }
                        if (now - since >= graceMs) ended.add(pkg)
                    }
                }
                now - seen >= neverOpenedTimeoutMs -> ended.add(pkg)
            }
        }
        return ended
    }
}
