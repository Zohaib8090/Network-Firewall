package dev.zohaib.networkfirewall.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.zohaib.networkfirewall.data.db.AppDatabase
import dev.zohaib.networkfirewall.data.repository.FirewallRepository
import dev.zohaib.networkfirewall.vpn.FirewallVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirewallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(packageName.hashCode())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                when (action) {
                    ACTION_ALLOW_10M -> {
                        val until = System.currentTimeMillis() + (10 * 60 * 1000L)
                        db.appRuleDao().setTemporaryAccess(packageName, until)
                        FirewallVpnService.reload(context)
                    }
                    ACTION_ALLOW_30M -> {
                        val until = System.currentTimeMillis() + (30 * 60 * 1000L)
                        db.appRuleDao().setTemporaryAccess(packageName, until)
                        FirewallVpnService.reload(context)
                    }
                    ACTION_ALLOW_SESSION -> {
                        FirewallRepository(context).allowWhileOpen(packageName)
                        FirewallVpnService.reload(context)
                    }
                    ACTION_ALWAYS_ALLOW -> {
                        db.appRuleDao().updateToggles(packageName, blockWifi = false, blockMobile = false)
                        FirewallVpnService.reload(context)
                    }
                    ACTION_KEEP_BLOCKED -> {
                        // Stay blocked and stop asking until this app has been closed and reopened
                        FirewallVpnService.suppressPrompt(packageName)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package_name"
        const val ACTION_ALLOW_10M = "dev.zohaib.networkfirewall.action.ALLOW_10M"
        const val ACTION_ALLOW_30M = "dev.zohaib.networkfirewall.action.ALLOW_30M"
        const val ACTION_ALLOW_SESSION = "dev.zohaib.networkfirewall.action.ALLOW_SESSION"
        const val ACTION_ALWAYS_ALLOW = "dev.zohaib.networkfirewall.action.ALWAYS_ALLOW"
        const val ACTION_KEEP_BLOCKED = "dev.zohaib.networkfirewall.action.KEEP_BLOCKED"
    }
}
