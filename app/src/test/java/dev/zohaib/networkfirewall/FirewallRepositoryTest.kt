package dev.zohaib.networkfirewall

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import dev.zohaib.networkfirewall.data.db.AppDatabase
import dev.zohaib.networkfirewall.data.model.BlockLog
import dev.zohaib.networkfirewall.data.repository.FirewallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirewallRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun cleanDatabase() = runBlocking {
        withContext(Dispatchers.IO) { db.clearAllTables() }
    }

    private fun install(packageName: String, system: Boolean = false) {
        val info = PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                name = packageName
                flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
            }
        }
        shadowOf(context.packageManager).installPackage(info)
    }

    private suspend fun rules() = db.appRuleDao().getAllRules().first()

    @Test
    fun `first sync lists every app and blocks nothing`() = runBlocking {
        install("com.example.chat")
        install("com.example.game")
        install("com.android.settings", system = true)

        FirewallRepository(context).syncInstalledApps()

        val rules = rules()
        assertTrue(rules.map { it.packageName }.containsAll(listOf("com.example.chat", "com.example.game", "com.android.settings")))
        assertTrue("no app may be blocked by default", rules.none { it.isWifiBlocked || it.isMobileBlocked })
    }

    @Test
    fun `syncing again keeps the blocks the user chose`() = runBlocking {
        install("com.example.chat")
        install("com.example.game")
        val repo = FirewallRepository(context)
        repo.syncInstalledApps()
        repo.updateToggles("com.example.chat", blockWifi = true, blockMobile = false)

        repo.syncInstalledApps()

        val chat = rules().first { it.packageName == "com.example.chat" }
        val game = rules().first { it.packageName == "com.example.game" }
        assertTrue(chat.isWifiBlocked)
        assertFalse(chat.isMobileBlocked)
        assertFalse(game.isWifiBlocked || game.isMobileBlocked)
    }

    @Test
    fun `a newly installed app starts unblocked`() = runBlocking {
        val repo = FirewallRepository(context)
        install("com.example.newapp")

        repo.onPackageAdded("com.example.newapp")

        val rule = rules().first { it.packageName == "com.example.newapp" }
        assertEquals(false to false, rule.isWifiBlocked to rule.isMobileBlocked)
    }

    @Test
    fun `keepNewest trims the block log to the newest entries`() = runBlocking {
        val dao = db.blockLogDao()
        for (t in 1L..5L) {
            dao.insertLog(BlockLog(packageName = "a", appName = "A", timestamp = t, networkType = "WIFI", reason = "test"))
        }

        dao.keepNewest(2)

        assertEquals(listOf(5L, 4L), dao.getRecentLogs(100).first().map { it.timestamp })
    }

    @Test
    fun `keepNewest leaves a small log alone`() = runBlocking {
        val dao = db.blockLogDao()
        dao.insertLog(BlockLog(packageName = "a", appName = "A", timestamp = 1, networkType = "WIFI", reason = "test"))

        dao.keepNewest(2000)

        assertEquals(1, dao.getRecentLogs(100).first().size)
    }
}
