package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    allAppUsages: List<AppUsageInfo> = appUsages,
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
    onTogglePin: (String, Boolean) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeLimitRule by remember { mutableStateOf<AppRule?>(null) }

    val isRunning = vpnStatus == VpnStatus.ACTIVE || vpnStatus == VpnStatus.GLOBAL_LOCKED
    val isPaused = vpnStatus == VpnStatus.PAUSED

    val totalBlockedCount = remember(allAppUsages) {
        allAppUsages.count { it.rule.isWifiBlocked || it.rule.isMobileBlocked }
    }
    val userCount = remember(allAppUsages) { allAppUsages.count { !it.isSystemApp } }
    val systemCount = remember(allAppUsages) { allAppUsages.count { it.isSystemApp } }

    // Infinite pulse animation for hero card
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("firewall_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Premium Firewall Status Hero Card
        item {
            val heroGradient = when {
                isGlobalLock -> Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                )
                isPaused -> Brush.linearGradient(
                    colors = listOf(
                        StatusPaused.copy(alpha = 0.25f),
                        StatusPaused.copy(alpha = 0.08f)
                    )
                )
                isRunning -> Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                else -> Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firewall_status_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroGradient)
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Animated icon with pulsing ring
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRunning) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .scale(pulseScale)
                                            .background(
                                                if (isGlobalLock) StatusBlocked.copy(alpha = 0.18f)
                                                else StatusAllowed.copy(alpha = 0.18f),
                                                CircleShape
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
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
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
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
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        isGlobalLock -> "All apps isolated locally"
                                        isPaused -> "All traffic temporarily permitted"
                                        isRunning -> "$totalBlockedCount apps filtered • ${allAppUsages.size} total"
                                        else -> "Tap to enable protection"
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
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Stats pills row
                        if (isRunning) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill(label = "Blocked", value = "$totalBlockedCount", color = StatusBlocked)
                                StatPill(label = "Users", value = "$userCount", color = StatusAllowed)
                                StatPill(label = "System", value = "$systemCount", color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Quick Pause / Resume Controls
                        AnimatedVisibility(visible = isRunning || isPaused) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isPaused) {
                                        Button(
                                            onClick = onResumeVpn,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Resume Guard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onPauseVpn(15) },
                                            modifier = Modifier.weight(1f).testTag("btn_pause_15m"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pause 15m", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { onPauseVpn(60) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
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
                        AppFilter.ALL to "All (${allAppUsages.size})",
                        AppFilter.USER to "User ($userCount)",
                        AppFilter.SYSTEM to "System ($systemCount)",
                        AppFilter.BLOCKED to "Blocked ($totalBlockedCount)"
                    )
                    items(filters) { (filter, label) ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterChange(filter) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_apps_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing…", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Apps", fontSize = 12.sp)
                        }
                    }
                    Row {
                        TextButton(onClick = onBlockAll) {
                            Text("Block All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onAllowAll) {
                            Text("Allow All", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Shimmer skeleton while refreshing
        if (isRefreshing && appUsages.isEmpty()) {
            items(8) {
                ShimmerAppItemSkeleton(shimmerOffset)
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
                onResetRuleClick = { onResetRule(app.packageName) },
                onTogglePin = { isPinned -> onTogglePin(app.packageName, isPinned) }
            )
        }

        // Animated empty state
        if (appUsages.isEmpty() && !isRefreshing) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = "No apps match your filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try a different search or filter option",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = color.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ShimmerAppItemSkeleton(shimmerOffset: Float) {
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.08f),
            Color.Gray.copy(alpha = 0.2f),
            Color.Gray.copy(alpha = 0.08f)
        ),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset + 300f, 0f)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(shimmerBrush))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Box(modifier = Modifier.fillMaxWidth(0.75f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            }
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
        }
    }
}
