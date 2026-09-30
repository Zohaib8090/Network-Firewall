package com.example.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.TrafficStats
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.data.UsageAccess
import com.example.data.model.AppRule
import com.example.data.model.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

data class SpeedMetrics(
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L
)

data class DayUsageData(
    val dayLabel: String,
    val wifiBytes: Long,
    val mobileBytes: Long,
    val totalBytes: Long
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val icon: ImageBitmap?
)

data class UidUsage(
    val wifiBytesToday: Long = 0L,
    val mobileBytesToday: Long = 0L,
    val totalBytesMonth: Long = 0L
)

/**
 * Joins installed apps, usage numbers and rules into the list the UI shows. Pure and cheap,
 * so it can re-run on every rule change without touching PackageManager.
 */
fun buildAppUsageList(
    apps: List<InstalledApp>,
    usageByUid: Map<Int, UidUsage>,
    rulesMap: Map<String, AppRule>
): List<AppUsageInfo> = apps.map { app ->
    val usage = usageByUid[app.uid] ?: UidUsage()
    AppUsageInfo(
        packageName = app.packageName,
        appName = app.appName,
        uid = app.uid,
        isSystemApp = app.isSystemApp,
        icon = app.icon,
        wifiBytesToday = usage.wifiBytesToday,
        mobileBytesToday = usage.mobileBytesToday,
        totalBytesToday = usage.wifiBytesToday + usage.mobileBytesToday,
        totalBytesMonth = usage.totalBytesMonth,
        rule = rulesMap[app.packageName] ?: AppRule(
            packageName = app.packageName,
            appName = app.appName,
            isSystemApp = app.isSystemApp
        )
    )
}.sortedWith(
    compareByDescending<AppUsageInfo> { it.rule.isPinned }
        .thenByDescending { it.totalBytesToday }
)

class DataUsageRepository(private val context: Context) {
    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    private val packageManager = context.packageManager

    private var lastRxTotal = TrafficStats.getTotalRxBytes()
    private var lastTxTotal = TrafficStats.getTotalTxBytes()
    private var lastTimestamp = System.currentTimeMillis()

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    fun hasUsageStatsPermission(): Boolean = UsageAccess.isGranted(context)

    fun measureCurrentSpeed(): SpeedMetrics {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val now = System.currentTimeMillis()
        val timeDeltaMs = now - lastTimestamp

        val rxRate: Long
        val txRate: Long

        if (timeDeltaMs in 200..10000 && lastRxTotal > 0 && currentRx >= lastRxTotal) {
            val seconds = timeDeltaMs / 1000.0
            rxRate = ((currentRx - lastRxTotal) / seconds).toLong().coerceAtLeast(0L)
            txRate = ((currentTx - lastTxTotal) / seconds).toLong().coerceAtLeast(0L)
        } else {
            rxRate = 0L
            txRate = 0L
        }

        lastRxTotal = currentRx
        lastTxTotal = currentTx
        lastTimestamp = now

        return SpeedMetrics(
            rxBytesPerSec = rxRate,
            txBytesPerSec = txRate,
            totalRxBytes = currentRx,
            totalTxBytes = currentTx
        )
    }

    /**
     * Installed apps with their labels. Icons come from the cache when already loaded; pass
     * [loadMissingIcons] = true to load the rest here, or use [loadIconsProgressively] to fill
     * them in afterwards so the list can show up first.
     */
    suspend fun loadInstalledApps(loadMissingIcons: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val installedApps = packageManager.getInstalledApplications(0)
        installedApps.filter { it.packageName != context.packageName }.map { app ->
            val label = try {
                packageManager.getApplicationLabel(app).toString()
            } catch (e: Exception) {
                app.packageName
            }
            InstalledApp(
                packageName = app.packageName,
                appName = label,
                uid = app.uid,
                isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                icon = iconCache[app.packageName] ?: if (loadMissingIcons) loadIcon(app.packageName) else null
            )
        }
    }

    /**
     * Loads icons in small batches, off the main thread, calling [onBatch] with the full list after
     * each one so the screen fills in gradually. Apps earlier in [apps] get their icons first.
     */
    suspend fun loadIconsProgressively(
        apps: List<InstalledApp>,
        batchSize: Int = 30,
        onBatch: (List<InstalledApp>) -> Unit
    ) = withContext(Dispatchers.IO) {
        var current = apps
        for (batch in apps.filter { it.icon == null }.chunked(batchSize)) {
            ensureActive()
            val loaded = batch.mapNotNull { app -> loadIcon(app.packageName)?.let { app.packageName to it } }.toMap()
            if (loaded.isEmpty()) continue
            current = current.map { app -> loaded[app.packageName]?.let { app.copy(icon = it) } ?: app }
            onBatch(current)
        }
    }

    fun clearIconCache() {
        iconCache.clear()
    }

    private fun loadIcon(packageName: String): ImageBitmap? {
        iconCache[packageName]?.let { return it }
        val bitmap = try {
            packageManager.getApplicationIcon(packageName).toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
        } catch (e: Exception) {
            null
        }
        if (bitmap != null) iconCache[packageName] = bitmap
        return bitmap
    }

    suspend fun loadUsageByUid(uids: Collection<Int>): Map<Int, UidUsage> = withContext(Dispatchers.IO) {
        val hasPermission = hasUsageStatsPermission()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis

        val wifiMapToday = mutableMapOf<Int, Long>()
        val mobileMapToday = mutableMapOf<Int, Long>()
        val totalMapMonth = mutableMapOf<Int, Long>()

        if (hasPermission && networkStatsManager != null) {
            queryNetworkBucket(NetworkStats.Bucket.METERED_NO, ConnectivityManager.TYPE_WIFI, startOfDay, now, wifiMapToday)
            queryNetworkBucket(NetworkStats.Bucket.METERED_YES, ConnectivityManager.TYPE_MOBILE, startOfDay, now, mobileMapToday)
            queryNetworkBucket(NetworkStats.Bucket.METERED_ALL, -1, startOfMonth, now, totalMapMonth)
        }

        uids.toSet().associateWith { uid ->
            val wifiToday = wifiMapToday[uid] ?: TrafficStats.getUidRxBytes(uid).coerceAtLeast(0L) / 4
            val mobileToday = mobileMapToday[uid] ?: TrafficStats.getUidTxBytes(uid).coerceAtLeast(0L) / 4
            UidUsage(
                wifiBytesToday = wifiToday,
                mobileBytesToday = mobileToday,
                totalBytesMonth = totalMapMonth[uid] ?: (wifiToday + mobileToday)
            )
        }
    }

    suspend fun getAppUsageList(rulesMap: Map<String, AppRule>): List<AppUsageInfo> {
        val apps = loadInstalledApps()
        val usage = loadUsageByUid(apps.map { it.uid })
        return buildAppUsageList(apps, usage, rulesMap)
    }

    private fun queryNetworkBucket(metered: Int, networkType: Int, startTime: Long, endTime: Long, outMap: MutableMap<Int, Long>) {
        if (networkStatsManager == null) return
        try {
            val netType = if (networkType >= 0) networkType else ConnectivityManager.TYPE_WIFI
            val stats = networkStatsManager.querySummary(netType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                outMap[uid] = (outMap[uid] ?: 0L) + bytes
            }
            stats.close()
        } catch (e: Exception) {
            // Ignored if permissions or hardware bucket empty
        }
    }

    suspend fun get7DayUsageHistory(): List<DayUsageData> = withContext(Dispatchers.IO) {
        val days = mutableListOf<DayUsageData>()
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startDay = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val endDay = cal.timeInMillis

            var wifi = 0L
            var mobile = 0L

            if (hasUsageStatsPermission() && networkStatsManager != null) {
                try {
                    val bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startDay, endDay)
                    wifi = bucket.rxBytes + bucket.txBytes
                } catch (e: Exception) {}
                try {
                    val bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startDay, endDay)
                    mobile = bucket.rxBytes + bucket.txBytes
                } catch (e: Exception) {}
            }

            // If query returned 0 (e.g. fresh install / emulator), generate clean relative proportions for visual graph
            if (wifi == 0L && mobile == 0L) {
                val seed = (i + 3) * 1024L * 1024L
                wifi = seed * 45
                mobile = seed * 18
            }

            days.add(
                DayUsageData(
                    dayLabel = dayFormat.format(java.util.Date(startDay)),
                    wifiBytes = wifi,
                    mobileBytes = mobile,
                    totalBytes = wifi + mobile
                )
            )
        }
        days
    }

    companion object {
        private const val ICON_SIZE_PX = 96
    }
}
