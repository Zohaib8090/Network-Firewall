package dev.zohaib.networkfirewall.vpn

/** Decides whether a blocked attempt may raise the "Allow it?" notification. Pure so it can be unit-tested. */
object PromptPolicy {
    /**
     * Only the app in front gets a prompt, never a system app (the user shouldn't be asked to
     * allow or keep blocking parts of Android itself), never one the user already chose
     * "Keep blocked" for, and at most once per [throttleMs].
     */
    fun shouldPrompt(
        isFrontApp: Boolean,
        isSystemApp: Boolean,
        isSuppressed: Boolean,
        millisSinceLastPrompt: Long,
        throttleMs: Long
    ): Boolean = isFrontApp && !isSystemApp && !isSuppressed && millisSinceLastPrompt >= throttleMs
}
