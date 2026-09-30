package dev.zohaib.networkfirewall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.zohaib.networkfirewall.vpn.FirewallVpnService

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        FirewallVpnService.reload(context)
    }
}
