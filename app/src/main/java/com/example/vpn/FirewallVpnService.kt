package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.BlockLog
import com.example.data.preferences.AppPreferences
import com.example.receiver.FirewallActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.Calendar

class FirewallVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetDropperJob: Job? = null
    private var connectivityManager: ConnectivityManager? = null
    private var isCurrentWifi = false

    private val database by lazy { AppDatabase.getInstance(this) }
    private val appPreferences by lazy { AppPreferences(this) }

    private val recentBlockedAlertTimestamps = mutableMapOf<String, Long>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            if (isWifi != isCurrentWifi) {
                isCurrentWifi = isWifi
                serviceScope.launch {
                    reconfigureVpn()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification("Smart Network Guard active", 0))
                serviceScope.launch {
                    appPreferences.setVpnEnabled(true)
                    reconfigureVpn()
                }
            }
            ACTION_RELOAD -> {
                serviceScope.launch {
                    reconfigureVpn()
                }
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    appPreferences.setVpnEnabled(false)
                    stopVpn()
                }
            }
            ACTION_PAUSE_15M -> {
                serviceScope.launch {
                    val until = System.currentTimeMillis() + 15 * 60 * 1000L
                    appPreferences.setPausedUntil(until)
                    reconfigureVpn()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun reconfigureVpn() {
        val isPausedUntil = appPreferences.pausedUntil.first()
        val now = System.currentTimeMillis()
        val isPaused = isPausedUntil > now

        val isGlobalLock = appPreferences.isGlobalInternetLock.first()

        if (isPaused) {
            _vpnState.value = VpnStatus.PAUSED
            teardownTunnel()
            updateForegroundNotification("Firewall paused for temporary access", 0)
            return
        }

        if (isGlobalLock) {
            _vpnState.value = VpnStatus.GLOBAL_LOCKED
        } else {
            _vpnState.value = VpnStatus.ACTIVE
        }

        // Determine current network type
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: true
        isCurrentWifi = isWifi

        // Fetch rules
        val allRules = database.appRuleDao().getAllRules().first()
        val activeSchedules = database.scheduleRuleDao().getActiveSchedules()

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> "MON"
        }

        val scheduledBlockedPackages = mutableSetOf<String>()
        for (schedule in activeSchedules) {
            if (schedule.isActiveAt(hour, minute, dayOfWeek)) {
                val shouldBlockOnCurrent = if (isWifi) schedule.blockWifi else schedule.blockMobile
                if (shouldBlockOnCurrent) {
                    if (schedule.targetPackageNames.trim() == "ALL") {
                        allRules.forEach { scheduledBlockedPackages.add(it.packageName) }
                    } else {
                        schedule.targetPackageNames.split(",").forEach { pkg ->
                            val cleanPkg = pkg.trim()
                            if (cleanPkg.isNotEmpty()) scheduledBlockedPackages.add(cleanPkg)
                        }
                    }
                }
            }
        }

        // Collect blocked packages
        val blockedPackages = mutableSetOf<String>()
        if (isGlobalLock) {
            val pm = packageManager
            val installed = pm.getInstalledApplications(0)
            for (app in installed) {
                if (app.packageName != packageName) {
                    blockedPackages.add(app.packageName)
                }
            }
        } else {
            for (rule in allRules) {
                if (rule.packageName == packageName) continue
                val ruleBlocked = rule.isEffectivelyBlocked(isWifi, now)
                val scheduleBlocked = scheduledBlockedPackages.contains(rule.packageName)
                if (ruleBlocked || scheduleBlocked) {
                    blockedPackages.add(rule.packageName)
                }
            }
        }

        teardownTunnel()

        if (blockedPackages.isEmpty()) {
            updateForegroundNotification("Firewall Active - All traffic permitted", 0)
            return
        }

        try {
            val builder = Builder()
            builder.setSession("Smart Network Guard")
            builder.setMtu(1500)
            builder.addAddress("10.255.255.1", 30)
            builder.addRoute("0.0.0.0", 0)

            val configureIntent = Intent(this, MainActivity::class.java)
            val pendingConfig = PendingIntent.getActivity(
                this, 0, configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setConfigureIntent(pendingConfig)

            // Add blocked applications to be routed to our dummy local sink
            for (pkg in blockedPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                } catch (e: PackageManager.NameNotFoundException) {
                    // Package may have been uninstalled
                }
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                startPacketDropper(vpnInterface!!, blockedPackages)
                val statusText = if (isGlobalLock) {
                    "GLOBAL INTERNET LOCK: All apps isolated"
                } else {
                    "Protected: ${blockedPackages.size} apps blocked on ${if (isWifi) "Wi-Fi" else "Mobile"}"
                }
                updateForegroundNotification(statusText, blockedPackages.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPacketDropper(pfd: ParcelFileDescriptor, blockedPackages: Set<String>) {
        packetDropperJob?.cancel()
        packetDropperJob = serviceScope.launch(Dispatchers.IO) {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32768)

            try {
                while (isActive) {
                    buffer.clear()
                    val length = inputStream.read(buffer.array())
                    if (length <= 0) continue

                    // Parse IP Header for diagnostics & blocked attempt notification
                    val version = (buffer.get(0).toInt() shr 4) and 0x0F
                    if (version == 4 && length >= 20) {
                        val protocol = buffer.get(9).toInt() and 0xFF
                        val destIpBytes = ByteArray(4)
                        buffer.position(16)
                        buffer.get(destIpBytes)
                        val destIp = InetAddress.getByAddress(destIpBytes).hostAddress ?: ""

                        var destPort = 0
                        if ((protocol == 6 || protocol == 17) && length >= 24) { // TCP or UDP
                            buffer.position(22)
                            destPort = buffer.short.toInt() and 0xFFFF
                        }

                        // Inspect blocked attempt
                        val firstBlocked = blockedPackages.firstOrNull() ?: "unknown"
                        handleBlockedAttempt(firstBlocked, destIp, destPort)
                    }
                    // Packet is consumed and not forwarded: completely dropped locally!
                }
            } catch (e: Exception) {
                // Stream closed or service stopping
            }
        }
    }

    private suspend fun handleBlockedAttempt(packageName: String, destIp: String, destPort: Int) {
        val now = System.currentTimeMillis()
        val lastAlert = recentBlockedAlertTimestamps[packageName] ?: 0L
        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val netType = if (isCurrentWifi) "WIFI" else "CELLULAR"

        // Record in Database
        database.blockLogDao().insertLog(
            BlockLog(
                packageName = packageName,
                appName = appName,
                timestamp = now,
                networkType = netType,
                reason = if (_vpnState.value == VpnStatus.GLOBAL_LOCKED) "Global Lock" else "Firewall Rule",
                ipAddress = destIp,
                port = destPort
            )
        )

        // Emit to Live UI
        val event = BlockedAttemptEvent(
            packageName = packageName,
            appName = appName,
            timestamp = now,
            networkType = netType,
            destIp = destIp,
            destPort = destPort
        )
        _blockedEventsFlow.emit(event)

        // Throttle notifications to once per 10 seconds per package
        if (now - lastAlert > 10_000L) {
            recentBlockedAlertTimestamps[packageName] = now
            val notifyAllowed = appPreferences.notifyBlockedAttempts.first()
            if (notifyAllowed) {
                showBlockedAttemptNotification(packageName, appName, netType)
            }
        }
    }

    private fun showBlockedAttemptNotification(packageName: String, appName: String, networkType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("prompt_package", packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, packageName.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick action: Allow 10 min
        val allow10mIntent = Intent(this, FirewallActionReceiver::class.java).apply {
            action = FirewallActionReceiver.ACTION_ALLOW_10M
            putExtra(FirewallActionReceiver.EXTRA_PACKAGE, packageName)
        }
        val allow10mPending = PendingIntent.getBroadcast(
            this, (packageName + "_10m").hashCode(), allow10mIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick action: Keep Blocked (dismiss)
        val dismissIntent = Intent(this, FirewallActionReceiver::class.java).apply {
            action = FirewallActionReceiver.ACTION_KEEP_BLOCKED
            putExtra(FirewallActionReceiver.EXTRA_PACKAGE, packageName)
        }
        val dismissPending = PendingIntent.getBroadcast(
            this, (packageName + "_keep").hashCode(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Blocked Access: $appName")
            .setContentText("Attempted connection on $networkType. Choose an action:")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Allow 10 Min", allow10mPending)
            .addAction(0, "Keep Blocked", dismissPending)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(packageName.hashCode(), notification)
    }

    private fun teardownTunnel() {
        packetDropperJob?.cancel()
        packetDropperJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {}
        vpnInterface = null
    }

    private fun stopVpn() {
        teardownTunnel()
        _vpnState.value = VpnStatus.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
        teardownTunnel()
        serviceScope.cancel()
        _vpnState.value = VpnStatus.STOPPED
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val fgChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Firewall Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent firewall active status"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Firewall Alerts & Popups",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for blocked connection attempts"
                enableVibration(true)
            }

            nm.createNotificationChannel(fgChannel)
            nm.createNotificationChannel(alertChannel)
        }
    }

    private fun buildForegroundNotification(contentText: String, blockedCount: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 100, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, FirewallVpnService::class.java).apply {
            action = ACTION_PAUSE_15M
        }
        val pendingPause = PendingIntent.getService(
            this, 101, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Smart Network Guard")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Pause 15m", pendingPause)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(text: String, blockedCount: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification(text, blockedCount))
    }

    companion object {
        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_RELOAD = "com.example.vpn.RELOAD"
        const val ACTION_STOP = "com.example.vpn.STOP"
        const val ACTION_PAUSE_15M = "com.example.vpn.PAUSE_15M"

        const val CHANNEL_ID_FOREGROUND = "smart_guard_vpn_status"
        const val CHANNEL_ID_ALERTS = "smart_guard_vpn_alerts"
        const val NOTIFICATION_ID_FOREGROUND = 1001

        private val _vpnState = MutableStateFlow(VpnStatus.STOPPED)
        val vpnState = _vpnState.asStateFlow()

        private val _blockedEventsFlow = MutableSharedFlow<BlockedAttemptEvent>(extraBufferCapacity = 64)
        val blockedEventsFlow = _blockedEventsFlow.asSharedFlow()

        fun start(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun reload(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_RELOAD
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
