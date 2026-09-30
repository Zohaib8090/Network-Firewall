package dev.zohaib.networkfirewall

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.zohaib.networkfirewall.data.preferences.AppPreferences
import dev.zohaib.networkfirewall.vpn.FirewallVpnService
import dev.zohaib.networkfirewall.vpn.VpnStatus
import dev.zohaib.networkfirewall.vpn.VpnStopReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirewallVpnServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearState() {
        FirewallVpnService.clearStopReason()
    }

    @Test
    fun `revoked firewall switches itself off and tells the user why`() = runBlocking {
        val prefs = AppPreferences(context)
        prefs.setVpnEnabled(true)
        val service = Robolectric.setupService(FirewallVpnService::class.java)
        assertNull(FirewallVpnService.stopReason.value)

        service.onRevoke()

        val reason = withTimeout(10_000) { FirewallVpnService.stopReason.first { it != null } }
        assertEquals(VpnStopReason.REVOKED, reason)
        assertEquals(VpnStatus.STOPPED, FirewallVpnService.vpnState.value)
        assertFalse("the enabled preference must be cleared so boot doesn't restart it", prefs.isVpnEnabled.first())

        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications
        assertTrue(
            "a 'Firewall stopped' notification should be posted",
            notifications.any { it.extras.getString(Notification.EXTRA_TITLE) == "Firewall stopped" }
        )
    }
}
