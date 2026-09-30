package dev.zohaib.networkfirewall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import dev.zohaib.networkfirewall.data.repository.FirewallRepository
import dev.zohaib.networkfirewall.vpn.FirewallVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Real-Time System Broadcast Sync (Primary)
 * Listens for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED, and ACTION_PACKAGE_REPLACED.
 *
 * Data Pipeline:
 * 1. Intercepts intent.data.schemeSpecificPart (packageName) and queries PackageManager for UID/meta.
 * 2. Inserts or deletes package metadata in Room DB asynchronously.
 * 3. Registers newly installed apps with nothing blocked; the user decides what to block.
 * 4. Notifies active VpnService memory cache to reload dynamic firewall filtering rules instantly.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        Log.d("PackageChangeReceiver", "Received action: $action for package: $packageName")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = FirewallRepository(context)

                when (action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            // Extract UID to verify package installation state
                            try {
                                val uid = context.packageManager.getPackageUid(packageName, 0)
                                Log.d("PackageChangeReceiver", "New package installed: $packageName, UID: $uid")
                            } catch (e: PackageManager.NameNotFoundException) {
                                Log.w("PackageChangeReceiver", "Package UID query failed for $packageName", e)
                            }
                            // New apps start with nothing blocked
                            repository.onPackageAdded(packageName)
                            // Notify active VpnService to update memory cache & filtering rules immediately
                            FirewallVpnService.reload(context)
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            Log.d("PackageChangeReceiver", "Package removed: $packageName")
                            repository.onPackageRemoved(packageName)
                            FirewallVpnService.reload(context)
                        }
                    }

                    Intent.ACTION_PACKAGE_REPLACED -> {
                        Log.d("PackageChangeReceiver", "Package replaced/updated: $packageName")
                        repository.onPackageReplaced(packageName)
                        FirewallVpnService.reload(context)
                    }
                }
            } catch (e: Exception) {
                Log.e("PackageChangeReceiver", "Error processing package broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
