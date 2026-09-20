package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppRule
import com.example.data.model.AppUsageInfo
import com.example.ui.AppFilter
import com.example.ui.components.AppRuleItem
import com.example.ui.components.DataLimitDialog
import com.example.ui.theme.StatusAllowed
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusPaused
import com.example.vpn.VpnStatus

@Composable
fun FirewallScreen(
    vpnStatus: VpnStatus,
    isGlobalLock: Boolean,
    appUsages: List<AppUsageInfo>,
    searchQuery: String,
    selectedFilter: AppFilter,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onPauseVpn: (minutes: Int) -> Unit,
    onResumeVpn: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onToggleWifi: (String, Boolean) -> Unit,
    onToggleMobile: (String, Boolean) -> Unit,
    onTempAccess: (String, Int) -> Unit,
    onAllowSession: (String) -> Unit,
    onUpdateLimits: (String, Long, Long, Long) -> Unit,
    onResetRule: (String) -> Unit,
    onBlockAll: () -> Unit,
    onAllowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeLimitRule by remember { mutableStateOf<AppRule?>(null) }

    val isRunning = vpnStatus == VpnStatus.ACTIVE || vpnStatus == VpnStatus.GLOBAL_LOCKED
    val isPaused = vpnStatus == VpnStatus.PAUSED

    val blockedAppsCount = remember(appUsages) {
        appUsages.count { it.rule.isWifiBlocked || it.rule.isMobileBlocked }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("firewall_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Firewall Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firewall_status_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isGlobalLock -> MaterialTheme.colorScheme.errorContainer
                        isPaused -> StatusPaused.copy(alpha = 0.2f)
                        isRunning -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    when {
                                        isGlobalLock -> MaterialTheme.colorScheme.error
                                        isPaused -> StatusPaused
                                        isRunning -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Shield else Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isGlobalLock -> "Global Internet Lock"
                                    isPaused -> "Firewall Paused"
                                    isRunning -> "Firewall Active"
                                    else -> "Firewall Inactive"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isGlobalLock -> MaterialTheme.colorScheme.onErrorContainer
                                    isPaused -> MaterialTheme.colorScheme.onSurface
                                    isRunning -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            Text(
                                text = when {
                                    isGlobalLock -> "All apps isolated locally"
                                    isPaused -> "All traffic temporarily permitted"
                                    isRunning -> "$blockedAppsCount apps filtered locally"
                                    else -> "Tap switch to enable protection"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isRunning,
                            onCheckedChange = { checked ->
                                if (checked) onStartVpn() else onStopVpn()
                            },
                            modifier = Modifier.testTag("switch_firewall_master"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Quick Pause / Batch Controls
                    AnimatedVisibility(visible = isRunning || isPaused) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isPaused) {
                                    Button(
                                        onClick = onResumeVpn,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resume Guard", fontSize = 12.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onPauseVpn(15) },
                                        modifier = Modifier.weight(1f).testTag("btn_pause_15m")
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pause 15m", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onPauseVpn(60) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Pause 1h", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search ${appUsages.size} installed apps…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_apps_input")
            )
        }

        // Filter Chips & Batch Toggles Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        AppFilter.ALL to "All (${appUsages.size})",
                        AppFilter.USER to "User (${appUsages.count { !it.isSystemApp }})",
                        AppFilter.SYSTEM to "System (${appUsages.count { it.isSystemApp }})",
                        AppFilter.BLOCKED to "Blocked ($blockedAppsCount)"
                    )
                    items(filters) { (filter, label) ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterChange(filter) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onBlockAll) {
                        Text("Block All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onAllowAll) {
                        Text("Allow All", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // App Rules List
        items(appUsages, key = { it.packageName }) { app ->
            AppRuleItem(
                appUsage = app,
                onToggleWifi = { blocked -> onToggleWifi(app.packageName, blocked) },
                onToggleMobile = { blocked -> onToggleMobile(app.packageName, blocked) },
                onSetLimitClick = { activeLimitRule = app.rule },
                onTempAccessClick = { min -> onTempAccess(app.packageName, min) },
                onAllowSessionClick = { onAllowSession(app.packageName) },
                onResetRuleClick = { onResetRule(app.packageName) }
            )
        }

        if (appUsages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No apps found matching criteria",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Active Data Limit Config Dialog
    activeLimitRule?.let { rule ->
        DataLimitDialog(
            rule = rule,
            onSaveLimits = { daily, weekly, monthly ->
                onUpdateLimits(rule.packageName, daily, weekly, monthly)
                activeLimitRule = null
            },
            onDismiss = { activeLimitRule = null }
        )
    }
}
