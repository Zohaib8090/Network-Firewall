package com.example

import com.example.data.model.AppRule
import com.example.data.model.ScheduleRule
import com.example.data.repository.InstalledApp
import com.example.data.repository.UidUsage
import com.example.data.repository.buildAppUsageList
import com.example.vpn.FirewallTimers
import com.example.vpn.PacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FirewallLogicTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun utcMillis(hour: Int, minute: Int, day: Int = 1): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.JANUARY, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `parses IPv4 TCP packet with options`() {
        val packet = ByteArray(28)
        packet[0] = 0x46 // version 4, IHL 6 (24-byte header)
        packet[9] = 6
        byteArrayOf(10, 255.toByte(), 255.toByte(), 1).copyInto(packet, 12)
        byteArrayOf(93, 184.toByte(), 216.toByte(), 34).copyInto(packet, 16)
        packet[24] = 0xC3.toByte(); packet[25] = 0x50 // src port 50000
        packet[26] = 0x01; packet[27] = 0xBB.toByte() // dest port 443

        val info = PacketParser.parse(packet, packet.size)!!
        assertEquals(6, info.protocol)
        assertEquals("10.255.255.1", info.srcIp.hostAddress)
        assertEquals("93.184.216.34", info.destIp.hostAddress)
        assertEquals(50000, info.srcPort)
        assertEquals(443, info.destPort)
    }

    @Test
    fun `parses IPv6 UDP packet`() {
        val packet = ByteArray(48)
        packet[0] = 0x60
        packet[6] = 17
        packet[23] = 1 // src ::1
        packet[24] = 0x20; packet[25] = 0x01; packet[39] = 0x53 // dest 2001::53
        packet[42] = 0x00; packet[43] = 0x35 // dest port 53

        val info = PacketParser.parse(packet, packet.size)!!
        assertEquals(17, info.protocol)
        assertEquals(53, info.destPort)
    }

    @Test
    fun `rejects truncated and unknown packets`() {
        assertNull(PacketParser.parse(ByteArray(10).also { it[0] = 0x45 }, 10))
        assertNull(PacketParser.parse(ByteArray(40), 40)) // version 0
    }

    @Test
    fun `next change picks earliest of pause, temporary access and schedule boundaries`() {
        val now = utcMillis(21, 0)
        val schedule = ScheduleRule(
            name = "Night", startHour = 22, startMinute = 0, endHour = 6, endMinute = 30,
            daysOfWeek = "MON", targetPackageNames = "ALL"
        )
        assertEquals(utcMillis(22, 0), FirewallTimers.nextChangeAt(now, 0L, emptyList(), listOf(schedule), utc))

        val tempRule = AppRule(packageName = "a", appName = "A", temporaryAccessUntil = utcMillis(21, 10))
        assertEquals(utcMillis(21, 10), FirewallTimers.nextChangeAt(now, 0L, listOf(tempRule), listOf(schedule), utc))

        assertEquals(utcMillis(21, 5), FirewallTimers.nextChangeAt(now, utcMillis(21, 5), listOf(tempRule), listOf(schedule), utc))
    }

    @Test
    fun `schedule end wakes one minute after inclusive end minute`() {
        val now = utcMillis(23, 0)
        val schedule = ScheduleRule(
            name = "Night", startHour = 22, startMinute = 0, endHour = 6, endMinute = 30,
            daysOfWeek = "MON", targetPackageNames = "ALL"
        )
        assertEquals(utcMillis(6, 31, day = 2), FirewallTimers.nextChangeAt(now, 0L, emptyList(), listOf(schedule), utc))
    }

    @Test
    fun `nothing pending returns null`() {
        val now = utcMillis(12, 0)
        val expired = AppRule(packageName = "a", appName = "A", temporaryAccessUntil = now - 1)
        val disabled = ScheduleRule(
            name = "Off", startHour = 13, startMinute = 0, endHour = 14, endMinute = 0,
            daysOfWeek = "MON", targetPackageNames = "ALL", isEnabled = false
        )
        assertNull(FirewallTimers.nextChangeAt(now, now - 1, listOf(expired), listOf(disabled), utc))
    }

    @Test
    fun `app list joins usage and rules, pinned first then by usage`() {
        val apps = listOf(
            InstalledApp("a", "A", uid = 1, isSystemApp = false, icon = null),
            InstalledApp("b", "B", uid = 2, isSystemApp = false, icon = null),
            InstalledApp("c", "C", uid = 3, isSystemApp = true, icon = null)
        )
        val usage = mapOf(1 to UidUsage(wifiBytesToday = 10), 2 to UidUsage(mobileBytesToday = 50))
        val rules = mapOf(
            "a" to AppRule(packageName = "a", appName = "A", isWifiBlocked = true, isPinned = true)
        )

        val list = buildAppUsageList(apps, usage, rules)

        assertEquals(listOf("a", "b", "c"), list.map { it.packageName })
        assertEquals(true, list[0].rule.isWifiBlocked)
        assertEquals(50L, list[1].totalBytesToday)
        assertEquals(0L, list[2].totalBytesToday) // no usage entry
        assertEquals("c", list[2].rule.packageName) // default rule when none stored
    }
}
