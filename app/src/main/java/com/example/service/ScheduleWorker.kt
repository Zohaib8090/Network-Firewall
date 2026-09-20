package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.repository.DataUsageRepository
import com.example.vpn.FirewallVpnService
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ScheduleWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val dataUsageRepo = DataUsageRepository(context)
        val prefs = AppPreferences(context)

        // 1. Reload firewall for schedule changes
        FirewallVpnService.reload(context)

        // 2. Check Data Usage caps & Thresholds
        val notifyThresholds = prefs.notifyThresholdAlerts.first()
        if (notifyThresholds && dataUsageRepo.hasUsageStatsPermission()) {
            val rules = db.appRuleDao().getAllRules().first()
            val rulesMap = rules.associateBy { it.packageName }
            val usages = dataUsageRepo.getAppUsageList(rulesMap)

            for (usage in usages) {
                val rule = usage.rule
                if (rule.dailyLimitBytes > 0L) {
                    val used = usage.totalBytesToday
                    val limit = rule.dailyLimitBytes
                    val ratio = used.toDouble() / limit.toDouble()

                    if (ratio >= 1.0) {
                        showLimitNotification(
                            usage.packageName,
                            usage.appName,
                            "100% Daily Limit Exceeded",
                            "${usage.appName} reached its limit of ${formatLimit(limit)}."
                        )
                    } else if (ratio >= 0.90) {
                        showLimitNotification(
                            usage.packageName,
                            usage.appName,
                            "90% Daily Limit Warning",
                            "${usage.appName} has used 90% of its data limit."
                        )
                    } else if (ratio >= 0.80) {
                        showLimitNotification(
                            usage.packageName,
                            usage.appName,
                            "80% Daily Limit Notice",
                            "${usage.appName} has consumed 80% of its allowed data."
                        )
                    }
                }
            }
        }

        return Result.success()
    }

    private fun showLimitNotification(pkg: String, appName: String, title: String, content: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smart_guard_limits"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Data Limit Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .build()

        nm.notify((pkg + "_limit").hashCode(), notification)
    }

    private fun formatLimit(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else "$mb MB"
    }

    companion object {
        private const val WORK_NAME = "smart_guard_periodic_worker"

        fun schedulePeriodicCheck(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
