package dev.zohaib.networkfirewall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.zohaib.networkfirewall.data.preferences.AppPreferences
import dev.zohaib.networkfirewall.vpn.FirewallVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = AppPreferences(context)
                    val autoStart = prefs.autoStartOnBoot.first()
                    val vpnWasEnabled = prefs.isVpnEnabled.first()

                    if (autoStart && vpnWasEnabled) {
                        FirewallVpnService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
