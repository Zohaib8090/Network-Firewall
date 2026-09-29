package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppRule
import com.example.data.model.AppUsageInfo
import com.example.data.model.BlockLog
import com.example.data.model.ProfileType
import com.example.data.model.ScheduleRule
import com.example.data.preferences.AppPreferences
import com.example.data.repository.DataUsageRepository
import com.example.data.repository.DayUsageData
import com.example.data.repository.FirewallRepository
import com.example.data.repository.InstalledApp
import com.example.data.repository.SpeedMetrics
import com.example.data.repository.UidUsage
import com.example.data.repository.buildAppUsageList
import com.example.service.ScheduleWorker
import com.example.vpn.BlockedAttemptEvent
import com.example.vpn.FirewallVpnService
import com.example.vpn.VpnStatus
import com.example.vpn.VpnStopReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL, USER, SYSTEM, BLOCKED
}

/**
 * A rule change shown in the UI before the database confirms it. Only the fields the user
 * touched are overridden, so unrelated database updates still show through.
 */
private data class PendingEdit(
    val wifiBlocked: Boolean? = null,
    val mobileBlocked: Boolean? = null,
    val pinned: Boolean? = null
) {
    fun applyTo(rule: AppRule): AppRule = rule.copy(
        isWifiBlocked = wifiBlocked ?: rule.isWifiBlocked,
        isMobileBlocked = mobileBlocked ?: rule.isMobileBlocked,
        isPinned = pinned ?: rule.isPinned
    )

    /** Drops fields the database already agrees with; null when nothing is left pending. */
    fun unconfirmedAgainst(rule: AppRule): PendingEdit? {
        val remaining = PendingEdit(
            wifiBlocked = wifiBlocked?.takeIf { it != rule.isWifiBlocked },
            mobileBlocked = mobileBlocked?.takeIf { it != rule.isMobileBlocked },
            pinned = pinned?.takeIf { it != rule.isPinned }
        )
        return if (remaining == PendingEdit()) null else remaining
    }

    fun merge(other: PendingEdit) = PendingEdit(
        wifiBlocked = other.wifiBlocked ?: wifiBlocked,
        mobileBlocked = other.mobileBlocked ?: mobileBlocked,
        pinned = other.pinned ?: pinned
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val firewallRepo = FirewallRepository(application)
    private val dataUsageRepo = DataUsageRepository(application)
    private val prefs = AppPreferences(application)

    val vpnStatus: StateFlow<VpnStatus> = FirewallVpnService.vpnState
    val stopReason: StateFlow<VpnStopReason?> = FirewallVpnService.stopReason
    val isGlobalInternetLock: StateFlow<Boolean> = prefs.isGlobalInternetLock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val activeProfile: StateFlow<String> = prefs.activeProfile.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "NORMAL"
    )
    val themeMode: StateFlow<String> = prefs.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM"
    )
    val accentIndex: StateFlow<Int> = prefs.accentColorIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val schedules: StateFlow<List<ScheduleRule>> = firewallRepo.allSchedules.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val recentLogs: StateFlow<List<BlockLog>> = firewallRepo.recentBlockLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val blockedCount: StateFlow<Int> = firewallRepo.blockedCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    // The app list is joined from three sources that change at different rates, so a rule
    // toggle only re-runs the cheap in-memory join instead of re-querying PackageManager.
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _usageByUid = MutableStateFlow<Map<Int, UidUsage>>(emptyMap())
    private val _dbRules = MutableStateFlow<Map<String, AppRule>>(emptyMap())
    private val _pendingEdits = MutableStateFlow<Map<String, PendingEdit>>(emptyMap())

    val appUsages: StateFlow<List<AppUsageInfo>> = combine(
        _installedApps, _usageByUid, _dbRules, _pendingEdits
    ) { apps, usage, rules, pending ->
        val effectiveRules = if (pending.isEmpty()) rules else rules.mapValues { (pkg, rule) ->
            pending[pkg]?.applyTo(rule) ?: rule
        }
        buildAppUsageList(apps, usage, effectiveRules)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    val filteredAppUsages: StateFlow<List<AppUsageInfo>> = combine(
        appUsages, _searchQuery, _selectedFilter
    ) { usages, query, filter ->
        usages.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.appName.contains(query, ignoreCase = true) ||
                    item.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !item.isSystemApp
                AppFilter.SYSTEM -> item.isSystemApp
                AppFilter.BLOCKED -> item.rule.isWifiBlocked || item.rule.isMobileBlocked
            }

            matchesQuery && matchesFilter
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sampled only while the Monitoring tab is collecting it
    val speedMetrics: StateFlow<SpeedMetrics> = flow {
        while (true) {
            emit(dataUsageRepo.measureCurrentSpeed())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpeedMetrics())

    private val _weeklyHistory = MutableStateFlow<List<DayUsageData>>(emptyList())
    val weeklyHistory: StateFlow<List<DayUsageData>> = _weeklyHistory.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(dataUsageRepo.hasUsageStatsPermission())
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _onDemandEvent = MutableStateFlow<BlockedAttemptEvent?>(null)
    val onDemandEvent: StateFlow<BlockedAttemptEvent?> = _onDemandEvent.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Schedule periodic check
        ScheduleWorker.schedulePeriodicCheck(application)

        // Load installed apps (labels, icons) and usage once up front
        viewModelScope.launch(Dispatchers.IO) {
            firewallRepo.syncInstalledApps()
            reloadInstalledApps()
        }

        // Observe rules from DB; only installs/uninstalls (a changed package set) reload app metadata
        viewModelScope.launch {
            var knownPackages: Set<String>? = null
            firewallRepo.allRules.collect { rules ->
                val rulesMap = rules.associateBy { it.packageName }
                _dbRules.value = rulesMap
                _pendingEdits.update { pending ->
                    pending.mapNotNull { (pkg, edit) ->
                        val dbRule = rulesMap[pkg] ?: return@mapNotNull null
                        edit.unconfirmedAgainst(dbRule)?.let { pkg to it }
                    }.toMap()
                }
                val packages = rulesMap.keys
                if (knownPackages != null && packages != knownPackages) {
                    launch(Dispatchers.IO) { reloadInstalledApps() }
                }
                knownPackages = packages
            }
        }

        // Weekly history load
        viewModelScope.launch(Dispatchers.IO) {
            _weeklyHistory.value = dataUsageRepo.get7DayUsageHistory()
        }

        // Listen to VPN blocked events for instant on-demand popups
        viewModelScope.launch {
            FirewallVpnService.blockedEventsFlow.collect { event ->
                _onDemandEvent.value = event
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun dismissStopReason() {
        FirewallVpnService.clearStopReason()
    }

    fun dismissOnDemandDialog() {
        _onDemandEvent.value = null
    }

    private suspend fun reloadInstalledApps() {
        val apps = dataUsageRepo.loadInstalledApps()
        _installedApps.value = apps
        _usageByUid.value = dataUsageRepo.loadUsageByUid(apps.map { it.uid })
        _hasUsagePermission.value = dataUsageRepo.hasUsageStatsPermission()
    }

    private suspend fun refreshUsage() {
        _usageByUid.value = dataUsageRepo.loadUsageByUid(_installedApps.value.map { it.uid })
        _hasUsagePermission.value = dataUsageRepo.hasUsageStatsPermission()
    }

    /** Shows [edit] immediately and returns the resulting rule, or null if the app has no rule yet. */
    private fun applyEditLocally(packageName: String, edit: PendingEdit): AppRule? {
        val dbRule = _dbRules.value[packageName] ?: return null
        var merged = edit
        _pendingEdits.update { pending ->
            merged = pending[packageName]?.merge(edit) ?: edit
            pending + (packageName to merged)
        }
        return merged.applyTo(dbRule)
    }

    private fun dropPendingEdit(packageName: String) {
        _pendingEdits.update { it - packageName }
    }

    fun toggleWifi(packageName: String, blocked: Boolean) {
        val rule = applyEditLocally(packageName, PendingEdit(wifiBlocked = blocked)) ?: return
        saveToggles(packageName, rule.isWifiBlocked, rule.isMobileBlocked)
    }

    fun toggleMobile(packageName: String, blocked: Boolean) {
        val rule = applyEditLocally(packageName, PendingEdit(mobileBlocked = blocked)) ?: return
        saveToggles(packageName, rule.isWifiBlocked, rule.isMobileBlocked)
    }

    private fun saveToggles(packageName: String, blockWifi: Boolean, blockMobile: Boolean) {
        viewModelScope.launch {
            try {
                firewallRepo.updateToggles(packageName, blockWifi = blockWifi, blockMobile = blockMobile)
                FirewallVpnService.reload(getApplication())
            } catch (e: Exception) {
                dropPendingEdit(packageName)
            }
        }
    }

    fun setTemporaryAccess(packageName: String, durationMinutes: Int) {
        viewModelScope.launch {
            firewallRepo.setTemporaryAccess(packageName, durationMinutes)
            FirewallVpnService.reload(getApplication())
            _onDemandEvent.value = null
        }
    }

    fun setAllowSession(packageName: String, allow: Boolean) {
        viewModelScope.launch {
            firewallRepo.setAllowSession(packageName, allow)
            FirewallVpnService.reload(getApplication())
            _onDemandEvent.value = null
        }
    }

    fun updateDataLimits(packageName: String, dailyBytes: Long, weeklyBytes: Long, monthlyBytes: Long) {
        viewModelScope.launch {
            firewallRepo.updateDataLimit(packageName, dailyBytes, weeklyBytes, monthlyBytes)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun resetAppRule(packageName: String) {
        applyEditLocally(packageName, PendingEdit(wifiBlocked = false, mobileBlocked = false))
        viewModelScope.launch {
            firewallRepo.updateToggles(packageName, blockWifi = false, blockMobile = false)
            firewallRepo.setTemporaryAccess(packageName, 0)
            firewallRepo.setAllowSession(packageName, false)
            firewallRepo.updateDataLimit(packageName, 0, 0, 0)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun setPinned(packageName: String, isPinned: Boolean) {
        applyEditLocally(packageName, PendingEdit(pinned = isPinned)) ?: return
        viewModelScope.launch {
            try {
                firewallRepo.setPinned(packageName, isPinned)
            } catch (e: Exception) {
                dropPendingEdit(packageName)
            }
        }
    }

    fun toggleGlobalInternetLock(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setGlobalInternetLock(enabled)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun pauseFirewall(minutes: Int) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + (minutes * 60 * 1000L)
            prefs.setPausedUntil(until)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun resumeFirewall() {
        viewModelScope.launch {
            prefs.setPausedUntil(0L)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun applyProfile(profileType: ProfileType) {
        viewModelScope.launch {
            prefs.setActiveProfile(profileType.name)
            firewallRepo.applyProfile(profileType)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun addOrUpdateSchedule(schedule: ScheduleRule) {
        viewModelScope.launch {
            if (schedule.id == 0L) {
                firewallRepo.addSchedule(schedule)
            } else {
                firewallRepo.updateSchedule(schedule)
            }
            FirewallVpnService.reload(getApplication())
        }
    }

    fun toggleSchedule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            firewallRepo.setScheduleEnabled(id, enabled)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            firewallRepo.deleteSchedule(id)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun blockAllApps() {
        viewModelScope.launch {
            firewallRepo.setAllBlocked(blockWifi = true, blockMobile = true)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun allowAllApps() {
        viewModelScope.launch {
            firewallRepo.setAllBlocked(blockWifi = false, blockMobile = false)
            FirewallVpnService.reload(getApplication())
        }
    }

    fun resetAllRulesToDefault() {
        viewModelScope.launch {
            firewallRepo.resetAllRules()
            FirewallVpnService.reload(getApplication())
        }
    }

    fun clearBlockLogs() {
        viewModelScope.launch {
            firewallRepo.clearBlockLogs()
        }
    }

    fun exportRules(): String? {
        var json: String? = null
        viewModelScope.launch {
            json = firewallRepo.exportRulesJson()
            _backupStatus.value = "Exported ${json?.length ?: 0} bytes of rules successfully"
        }
        return json
    }

    fun importRules(jsonString: String) {
        viewModelScope.launch {
            val result = firewallRepo.importRulesJson(jsonString)
            if (result.isSuccess) {
                _backupStatus.value = "Imported ${result.getOrNull()} rules successfully!"
                FirewallVpnService.reload(getApplication())
            } else {
                _backupStatus.value = "Failed to parse JSON: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun setAccentColor(index: Int) {
        viewModelScope.launch {
            prefs.setAccentColorIndex(index)
        }
    }

    /**
     * On-Resume Delta Sync (Background Catch-up):
     * Queries PackageManager.getInstalledPackages() in background I/O dispatcher
     * to catch edge-case updates or apps installed during system Doze mode,
     * merging differences with local Room DB without clearing user-configured rules.
     */
    fun syncDeltaInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            firewallRepo.syncInstalledApps(blockMobileForNewApps = true)
            // Also picks up a usage-access grant made in system settings
            refreshUsage()
        }
    }

    /**
     * Manual On-Demand Pull-to-Refresh:
     * Forces complete state re-query from PackageManager, syncs Room DB,
     * and updates UI StateFlow immediately.
     */
    fun triggerManualRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                firewallRepo.syncInstalledApps(blockMobileForNewApps = true)
                dataUsageRepo.clearIconCache()
                reloadInstalledApps()
                _weeklyHistory.value = dataUsageRepo.get7DayUsageHistory()
            } finally {
                delay(400) // Smooth UX feedback for pull-to-refresh
                _isRefreshing.value = false
            }
        }
    }
}
