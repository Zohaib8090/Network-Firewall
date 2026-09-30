package dev.zohaib.networkfirewall

import dev.zohaib.networkfirewall.data.model.AppRule
import dev.zohaib.networkfirewall.data.model.ScheduleRule
import dev.zohaib.networkfirewall.vpn.BlockPlanner
import dev.zohaib.networkfirewall.vpn.ForegroundEvent
import dev.zohaib.networkfirewall.vpn.ForegroundEventType.PAUSED
import dev.zohaib.networkfirewall.vpn.ForegroundEventType.RESUMED
import dev.zohaib.networkfirewall.vpn.ForegroundLogic
import dev.zohaib.networkfirewall.vpn.SessionEndPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingLogicTest {

    private val now = 1_000_000L

    private fun rule(
        pkg: String,
        wifi: Boolean = false,
        mobile: Boolean = false,
        allowedUntil: Long = 0L,
        session: Boolean = false
    ) = AppRule(
        packageName = pkg, appName = pkg, isWifiBlocked = wifi, isMobileBlocked = mobile,
        temporaryAccessUntil = allowedUntil, allowSession = session
    )

    private fun plan(
        rules: List<AppRule>,
        installed: List<String> = emptyList(),
        schedules: List<ScheduleRule> = emptyList(),
        global: Boolean = false,
        wifi: Boolean = true,
        hour: Int = 12,
        day: String = "MON"
    ) = BlockPlanner.blockedPackages(rules, installed, schedules, global, wifi, "me", now, hour, 0, day)

    private val workHours = ScheduleRule(
        name = "Work", startHour = 9, startMinute = 0, endHour = 17, endMinute = 0,
        daysOfWeek = "MON,TUE", targetPackageNames = "a,b"
    )

    // ---- what gets blocked

    @Test
    fun `nothing is blocked when no rule blocks anything`() {
        assertEquals(emptySet<String>(), plan(listOf(rule("a"), rule("b"))))
    }

    @Test
    fun `wifi and mobile blocks only apply on their own network`() {
        val rules = listOf(rule("a", wifi = true), rule("b", mobile = true))
        assertEquals(setOf("a"), plan(rules, wifi = true))
        assertEquals(setOf("b"), plan(rules, wifi = false))
    }

    @Test
    fun `global lock blocks every installed app except this one`() {
        val blocked = plan(emptyList(), installed = listOf("a", "b", "me"), global = true)
        assertEquals(setOf("a", "b"), blocked)
    }

    @Test
    fun `global lock lets through an app the user allowed for a while or while open`() {
        val rules = listOf(rule("a", allowedUntil = now + 1_000), rule("b", session = true), rule("c"))
        val blocked = plan(rules, installed = listOf("a", "b", "c", "d"), global = true)
        assertEquals(setOf("c", "d"), blocked)
    }

    @Test
    fun `an expired allowance no longer lets the app through`() {
        val rules = listOf(rule("a", allowedUntil = now - 1))
        assertEquals(setOf("a"), plan(rules, installed = listOf("a"), global = true))
    }

    @Test
    fun `a schedule blocks its target apps only while it is active`() {
        val rules = listOf(rule("a"), rule("b"), rule("c"))
        assertEquals(setOf("a", "b"), plan(rules, schedules = listOf(workHours), hour = 12, day = "MON"))
        assertEquals(emptySet<String>(), plan(rules, schedules = listOf(workHours), hour = 12, day = "SAT"))
        assertEquals(emptySet<String>(), plan(rules, schedules = listOf(workHours), hour = 20, day = "MON"))
    }

    @Test
    fun `an allowance overrides a schedule and the app's own blocks`() {
        val rules = listOf(rule("a", wifi = true, allowedUntil = now + 1_000), rule("b", wifi = true))
        val blocked = plan(rules, schedules = listOf(workHours.copy(targetPackageNames = "ALL")))
        assertEquals(setOf("b"), blocked)
    }

    // ---- which app is in front

    private fun ev(t: Long, pkg: String, type: dev.zohaib.networkfirewall.vpn.ForegroundEventType) = ForegroundEvent(t, pkg, type)

    @Test
    fun `closing an app to the home screen makes the launcher the front app`() {
        val events = listOf(ev(1, "whatsapp", RESUMED), ev(5, "whatsapp", PAUSED), ev(5, "launcher", RESUMED))
        assertEquals("launcher", ForegroundLogic.reduce(null, events))
    }

    @Test
    fun `switching apps keeps the newer one in front even if the older one pauses last`() {
        val events = listOf(ev(1, "a", RESUMED), ev(2, "b", RESUMED), ev(3, "a", PAUSED))
        assertEquals("b", ForegroundLogic.reduce(null, events))
    }

    @Test
    fun `nothing is in front once the front app pauses with no successor`() {
        assertNull(ForegroundLogic.reduce(null, listOf(ev(1, "a", RESUMED), ev(2, "a", PAUSED))))
    }

    @Test
    fun `no new events keep the previous answer`() {
        assertEquals("a", ForegroundLogic.reduce("a", emptyList()))
    }

    @Test
    fun `events are applied in time order and replaying them changes nothing`() {
        val events = listOf(ev(3, "a", PAUSED), ev(1, "a", RESUMED), ev(2, "b", RESUMED))
        val once = ForegroundLogic.reduce(null, events)
        assertEquals("b", once)
        assertEquals(once, ForegroundLogic.reduce(once, events))
    }

    // ---- when "allow while open" ends

    private val grace = 15_000L
    private val neverOpened = 600_000L

    @Test
    fun `a session ends after the grace period once its app has been closed`() {
        val policy = SessionEndPolicy(grace, neverOpened)
        assertTrue(policy.update(listOf("a"), "a", 0).isEmpty())          // app is open
        assertTrue(policy.update(listOf("a"), "launcher", 1_000).isEmpty()) // just closed
        assertTrue(policy.update(listOf("a"), "launcher", 15_999).isEmpty()) // still in the grace period
        assertEquals(listOf("a"), policy.update(listOf("a"), "launcher", 16_000))
    }

    @Test
    fun `coming back during the grace period keeps the session`() {
        val policy = SessionEndPolicy(grace, neverOpened)
        policy.update(listOf("a"), "a", 0)
        policy.update(listOf("a"), "camera", 1_000)
        policy.update(listOf("a"), "a", 10_000)                           // back from the camera
        assertTrue(policy.update(listOf("a"), "camera", 20_000).isEmpty()) // the timer restarted
        assertEquals(listOf("a"), policy.update(listOf("a"), "camera", 35_000))
    }

    @Test
    fun `a session for an app that was never opened ends after a long wait`() {
        val policy = SessionEndPolicy(grace, neverOpened)
        assertTrue(policy.update(listOf("a"), "launcher", 0).isEmpty())
        assertTrue(policy.update(listOf("a"), "launcher", 599_999).isEmpty())
        assertEquals(listOf("a"), policy.update(listOf("a"), "launcher", 600_000))
    }

    @Test
    fun `nothing ends while the front app is unknown`() {
        val policy = SessionEndPolicy(grace, neverOpened)
        policy.update(listOf("a"), "a", 0)
        assertTrue(policy.update(listOf("a"), null, 1_000_000).isEmpty())
    }

    @Test
    fun `each session is tracked on its own`() {
        val policy = SessionEndPolicy(grace, neverOpened)
        policy.update(listOf("a", "b"), "a", 0)
        policy.update(listOf("a", "b"), "b", 1_000)
        assertEquals(listOf("a"), policy.update(listOf("a", "b"), "b", 20_000))
    }
}
