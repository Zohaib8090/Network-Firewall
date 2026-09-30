package dev.zohaib.networkfirewall

import dev.zohaib.networkfirewall.vpn.PromptPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptPolicyTest {
    private fun ask(
        front: Boolean = true,
        system: Boolean = false,
        suppressed: Boolean = false,
        since: Long = 60_000
    ) = PromptPolicy.shouldPrompt(front, system, suppressed, since, 30_000)

    @Test fun `a normal front app is prompted`() = assertTrue(ask())

    @Test fun `a system app is never prompted`() = assertFalse(ask(system = true))

    @Test fun `background apps are never prompted`() = assertFalse(ask(front = false))

    @Test fun `keep blocked suppresses the prompt`() = assertFalse(ask(suppressed = true))

    @Test fun `prompts are throttled`() {
        assertFalse(ask(since = 29_999))
        assertTrue(ask(since = 30_000))
    }
}
