package dev.zohaib.networkfirewall

import dev.zohaib.networkfirewall.vpn.BlockLogThrottle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockLogThrottleTest {

    @Test
    fun `the front app is logged once per destination every 10 seconds`() {
        val throttle = BlockLogThrottle()
        assertTrue(throttle.shouldLog("chat", "1.1.1.1", 443, isForeground = true, now = 0))
        assertFalse(throttle.shouldLog("chat", "1.1.1.1", 443, isForeground = true, now = 9_999))
        assertTrue("another destination is a new entry", throttle.shouldLog("chat", "8.8.8.8", 53, isForeground = true, now = 1_000))
        assertTrue(throttle.shouldLog("chat", "1.1.1.1", 443, isForeground = true, now = 10_000))
    }

    @Test
    fun `a background app is logged once a minute whatever it contacts`() {
        val throttle = BlockLogThrottle()
        assertTrue(throttle.shouldLog("sync", "1.1.1.1", 443, isForeground = false, now = 0))
        assertFalse(throttle.shouldLog("sync", "8.8.8.8", 53, isForeground = false, now = 1_000))
        assertFalse(throttle.shouldLog("sync", "9.9.9.9", 80, isForeground = false, now = 59_999))
        assertTrue(throttle.shouldLog("sync", "1.1.1.1", 443, isForeground = false, now = 60_000))
    }

    @Test
    fun `background apps are throttled separately from each other`() {
        val throttle = BlockLogThrottle()
        assertTrue(throttle.shouldLog("a", "1.1.1.1", 443, isForeground = false, now = 0))
        assertTrue(throttle.shouldLog("b", "1.1.1.1", 443, isForeground = false, now = 10))
        assertFalse(throttle.shouldLog("a", "1.1.1.1", 443, isForeground = false, now = 20))
    }

    @Test
    fun `clearing out old entries keeps the recent ones`() {
        val throttle = BlockLogThrottle(maxEntries = 2)
        throttle.shouldLog("p1", "x", 1, isForeground = false, now = 0)
        throttle.shouldLog("p2", "x", 1, isForeground = false, now = 1_000)
        throttle.shouldLog("p3", "x", 1, isForeground = false, now = 100_000) // triggers the clean-up

        assertFalse("p3 is recent and must still be remembered", throttle.shouldLog("p3", "x", 1, isForeground = false, now = 100_500))
        assertTrue("p1 was old enough to be forgotten", throttle.shouldLog("p1", "x", 1, isForeground = false, now = 100_600))
    }
}
