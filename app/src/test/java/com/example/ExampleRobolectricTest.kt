package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AppRule
import com.example.data.model.BlockLog
import com.example.data.model.ProfileType
import com.example.data.model.ScheduleRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Network Guard", appName)
    }

    @Test
    fun `insert and query app rule in database`() = runBlocking {
        val rule = AppRule(
            packageName = "com.sample.browser",
            appName = "Sample Browser",
            isWifiBlocked = true,
            isMobileBlocked = false
        )

        database.appRuleDao().insertRule(rule)
        val loaded = database.appRuleDao().getRuleByPackage("com.sample.browser")
        assertNotNull(loaded)
        assertEquals("Sample Browser", loaded?.appName)
        assertTrue(loaded?.isWifiBlocked == true)
        assertFalse(loaded?.isMobileBlocked == true)
    }

    @Test
    fun `schedule rule insertion and queries`() = runBlocking {
        val schedule = ScheduleRule(
            name = "Night Focus",
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 30,
            daysOfWeek = "MON,TUE,WED",
            targetPackageNames = "ALL",
            blockWifi = true,
            blockMobile = true,
            isEnabled = true
        )

        val id = database.scheduleRuleDao().insertSchedule(schedule)
        val allSchedules = database.scheduleRuleDao().getAllSchedules().first()
        assertEquals(1, allSchedules.size)
        assertEquals("Night Focus", allSchedules[0].name)
    }

    @Test
    fun `block log records and limits correctly`() = runBlocking {
        val log = BlockLog(
            packageName = "com.bad.tracker",
            appName = "Bad Tracker",
            networkType = "CELLULAR",
            ipAddress = "192.0.2.1",
            port = 443,
            reason = "Mobile data blocked by firewall"
        )

        database.blockLogDao().insertLog(log)
        val recent = database.blockLogDao().getRecentLogs(10).first()
        assertEquals(1, recent.size)
        assertEquals("com.bad.tracker", recent[0].packageName)
    }

    @Test
    fun `profile types have correct metadata`() {
        assertEquals("Study / Work", ProfileType.WORK_STUDY.displayName)
        assertEquals("Gaming", ProfileType.GAMING.displayName)
        assertEquals("Offline Mode", ProfileType.OFFLINE_MODE.displayName)
    }
}
