package com.example.ui

import android.app.Activity
import android.content.Context
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OnDemandAccessBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.FirewallScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.screens.SettingsLogsScreen
import com.example.ui.theme.StatusAllowed
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusPaused
import com.example.vpn.FirewallVpnService
import com.example.vpn.VpnStatus

enum class ScreenTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FIREWALL("Firewall", Icons.Default.Shield),
    MONITORING("Monitor", Icons.Default.BarChart),
    SCHEDULES("Schedules", Icons.Default.Schedule),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val vpnStatus by viewModel.vpnStatus.collectAsStateWithLifecycle()
    val isGlobalLock by viewModel.isGlobalInternetLock.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentIndex by viewModel.accentIndex.collectAsStateWithLifecycle()

    val filteredApps by viewModel.filteredAppUsages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    val speedMetrics by viewModel.speedMetrics.collectAsStateWithLifecycle()
    val weeklyHistory by viewModel.weeklyHistory.collectAsStateWithLifecycle()
    val allAppUsages by viewModel.appUsages.collectAsStateWithLifecycle()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsStateWithLifecycle()

    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val blockedCount by viewModel.blockedCount.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val onDemandEvent by viewModel.onDemandEvent.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Pulsing animation for the status dot
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // VPN Permission Launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FirewallVpnService.start(context)
        }
    }

    val startVpnAction = {
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            FirewallVpnService.start(context)
        }
    }

    val statusColor = when (vpnStatus) {
        VpnStatus.ACTIVE -> StatusAllowed
        VpnStatus.GLOBAL_LOCKED -> StatusBlocked
        VpnStatus.PAUSED -> StatusPaused
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val isLive = vpnStatus == VpnStatus.ACTIVE || vpnStatus == VpnStatus.GLOBAL_LOCKED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animated status icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(statusColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer pulse ring when live
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(pulseScale)
                                        .background(statusColor.copy(alpha = 0.1f), CircleShape)
                                )
                            }
                            Icon(
                                imageVector = if (isGlobalLock) Icons.Default.Lock else Icons.Default.Security,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Smart Network Guard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pulsing status dot
                                Box(
                                    modifier = Modifier
                                        .size(if (isLive) 6.dp else 5.dp)
                                        .then(if (isLive) Modifier.scale(pulseScale) else Modifier)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when (vpnStatus) {
                                        VpnStatus.ACTIVE -> "Active · $activeProfile"
                                        VpnStatus.GLOBAL_LOCKED -> "Global Lock"
                                        VpnStatus.PAUSED -> "Paused"
                                        else -> "Idle"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                ScreenTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tab == ScreenTab.FIREWALL && blockedCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = StatusBlocked) {
                                            Text(
                                                blockedCount.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = {
                            Text(
                                tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * direction / 4 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally { -it * direction / 4 } + fadeOut(tween(180)))
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> FirewallScreen(
                        vpnStatus = vpnStatus,
                        isGlobalLock = isGlobalLock,
                        appUsages = filteredApps,
                        allAppUsages = allAppUsages,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onStartVpn = startVpnAction,
                        onStopVpn = { FirewallVpnService.stop(context) },
                        onPauseVpn = { minutes -> viewModel.pauseFirewall(minutes) },
                        onResumeVpn = { viewModel.resumeFirewall() },
                        onSearchChange = { query -> viewModel.setSearchQuery(query) },
                        onFilterChange = { filter -> viewModel.setSelectedFilter(filter) },
                        onToggleWifi = { pkg, blocked -> viewModel.toggleWifi(pkg, blocked) },
                        onToggleMobile = { pkg, blocked -> viewModel.toggleMobile(pkg, blocked) },
                        onTempAccess = { pkg, min -> viewModel.setTemporaryAccess(pkg, min) },
                        onAllowSession = { pkg -> viewModel.setAllowSession(pkg, true) },
                        onUpdateLimits = { pkg, daily, weekly, monthly -> viewModel.updateDataLimits(pkg, daily, weekly, monthly) },
                        onResetRule = { pkg -> viewModel.resetAppRule(pkg) },
                        onBlockAll = { viewModel.blockAllApps() },
                        onAllowAll = { viewModel.allowAllApps() },
                        onTogglePin = { pkg, isPinned -> viewModel.setPinned(pkg, isPinned) },
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.triggerManualRefresh() }
                    )
                    1 -> AnalyticsScreen(
                        speedMetrics = speedMetrics,
                        weeklyHistory = weeklyHistory,
                        appUsages = allAppUsages,
                        hasUsagePermission = hasUsagePermission
                    )
                    2 -> SchedulesScreen(
                        isGlobalLock = isGlobalLock,
                        activeProfile = activeProfile,
                        schedules = schedules,
                        onToggleGlobalLock = { locked -> viewModel.toggleGlobalInternetLock(locked) },
                        onApplyProfile = { profile -> viewModel.applyProfile(profile) },
                        onAddSchedule = { rule -> viewModel.addOrUpdateSchedule(rule) },
                        onToggleSchedule = { id, enabled -> viewModel.toggleSchedule(id, enabled) },
                        onDeleteSchedule = { id -> viewModel.deleteSchedule(id) }
                    )
                    3 -> SettingsLogsScreen(
                        recentLogs = recentLogs,
                        themeMode = themeMode,
                        accentIndex = accentIndex,
                        backupStatus = backupStatus,
                        onClearLogs = { viewModel.clearBlockLogs() },
                        onResetAllRules = { viewModel.resetAllRulesToDefault() },
                        onExportRules = { viewModel.exportRules() },
                        onImportRules = { json -> viewModel.importRules(json) },
                        onClearBackupStatus = { viewModel.clearBackupStatus() },
                        onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                        onSetAccentColor = { index -> viewModel.setAccentColor(index) },
                        onTempAccess = { pkg, min -> viewModel.setTemporaryAccess(pkg, min) }
                    )
                }
            }
        }
    }

    // On-Demand Access Popup Modal Bottom Sheet
    onDemandEvent?.let { event ->
        OnDemandAccessBottomSheet(
            event = event,
            onAllowSession = {
                viewModel.setAllowSession(event.packageName, true)
                viewModel.dismissOnDemandDialog()
            },
            onKeepBlocked = {
                viewModel.dismissOnDemandDialog()
            },
            onAlwaysAllow = {
                viewModel.toggleWifi(event.packageName, false)
                viewModel.toggleMobile(event.packageName, false)
                viewModel.dismissOnDemandDialog()
            },
            onTempAccess = { minutes ->
                viewModel.setTemporaryAccess(event.packageName, minutes)
                viewModel.dismissOnDemandDialog()
            },
            onDismiss = {
                viewModel.dismissOnDemandDialog()
            }
        )
    }
}
