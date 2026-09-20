package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileType
import com.example.data.model.ScheduleRule
import com.example.ui.components.ScheduleRuleDialog

@Composable
fun SchedulesScreen(
    isGlobalLock: Boolean,
    activeProfile: String,
    schedules: List<ScheduleRule>,
    onToggleGlobalLock: (Boolean) -> Unit,
    onApplyProfile: (ProfileType) -> Unit,
    onAddSchedule: (ScheduleRule) -> Unit,
    onToggleSchedule: (Long, Boolean) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleRule?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("schedules_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global Internet Lock Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_lock_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGlobalLock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isGlobalLock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isGlobalLock) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Global Internet Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGlobalLock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Instantly suspend all device network traffic",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isGlobalLock,
                            onCheckedChange = onToggleGlobalLock,
                            modifier = Modifier.testTag("switch_global_lock"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onError,
                                checkedTrackColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            // Preset Profiles Header
            item {
                Text(
                    text = "Preset Profiles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Profiles Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val profiles = listOf(
                        Triple(ProfileType.NORMAL, Icons.Default.Shield, "Default app rules and user configuration"),
                        Triple(ProfileType.WORK_STUDY, Icons.Default.MenuBook, "Blocks social media, games, and streaming distractors"),
                        Triple(ProfileType.GAMING, Icons.Default.SportsEsports, "Restricts background sync and heavy downloaders to lower ping"),
                        Triple(ProfileType.BATTERY_SAVER, Icons.Default.BatteryAlert, "Restricts background cellular and wifi data for battery savings"),
                        Triple(ProfileType.OFFLINE_MODE, Icons.Default.WifiOff, "Full offline device lockdown")
                    )

                    profiles.forEach { (type, icon, desc) ->
                        val isSelected = activeProfile == type.name
                        OutlinedCard(
                            onClick = { onApplyProfile(type) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth().testTag("profile_${type.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = type.displayName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scheduled Rules Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Time-Based Schedules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            editingSchedule = null
                            showScheduleDialog = true
                        },
                        modifier = Modifier.testTag("btn_add_schedule")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Rule")
                    }
                }
            }

            // Schedule Rules List
            items(schedules, key = { it.id }) { schedule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingSchedule = schedule
                            showScheduleDialog = true
                        }
                        .testTag("schedule_item_${schedule.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = schedule.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            val timeText = String.format(
                                "%02d:%02d - %02d:%02d",
                                schedule.startHour, schedule.startMinute,
                                schedule.endHour, schedule.endMinute
                            )

                            Text(
                                text = "$timeText • ${schedule.daysOfWeek}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Restricts: ${if (schedule.blockWifi) "Wi-Fi" else ""} ${if (schedule.blockMobile) "Mobile" else ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = schedule.isEnabled,
                            onCheckedChange = { enabled -> onToggleSchedule(schedule.id, enabled) },
                            modifier = Modifier.testTag("switch_schedule_${schedule.id}")
                        )

                        IconButton(onClick = { onDeleteSchedule(schedule.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete schedule",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (schedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scheduled firewall rules set. Tap 'Add Rule' to create automatic time rules.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Extra space at bottom
            item { Spacer(modifier = Modifier.height(56.dp)) }
        }
    }

    if (showScheduleDialog) {
        ScheduleRuleDialog(
            initialRule = editingSchedule,
            onSaveSchedule = { rule ->
                onAddSchedule(rule)
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false }
        )
    }
}
