package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduleRule

@Composable
fun ScheduleRuleDialog(
    initialRule: ScheduleRule? = null,
    onSaveSchedule: (ScheduleRule) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "Night Isolation") }
    var startHour by remember { mutableIntStateOf(initialRule?.startHour ?: 23) }
    var startMinute by remember { mutableIntStateOf(initialRule?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialRule?.endHour ?: 7) }
    var endMinute by remember { mutableIntStateOf(initialRule?.endMinute ?: 0) }

    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val initialSelectedDays = initialRule?.daysOfWeek?.split(",")?.map { it.trim().uppercase() }?.toSet()
        ?: setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedDays by remember { mutableStateOf(initialSelectedDays) }

    var blockWifi by remember { mutableStateOf(initialRule?.blockWifi ?: true) }
    var blockMobile by remember { mutableStateOf(initialRule?.blockMobile ?: true) }
    var targetPackages by remember { mutableStateOf(initialRule?.targetPackageNames ?: "ALL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialRule != null) "Edit Schedule Rule" else "New Schedule Rule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_schedule_name")
                )

                // Time picker inputs
                Text("Active Hours (24-Hour Clock):", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", startHour, startMinute),
                        onValueChange = {
                            val parts = it.split(":")
                            if (parts.size == 2) {
                                startHour = (parts[0].toIntOrNull() ?: startHour).coerceIn(0, 23)
                                startMinute = (parts[1].toIntOrNull() ?: startMinute).coerceIn(0, 59)
                            }
                        },
                        label = { Text("From") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Text("to", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = String.format("%02d:%02d", endHour, endMinute),
                        onValueChange = {
                            val parts = it.split(":")
                            if (parts.size == 2) {
                                endHour = (parts[0].toIntOrNull() ?: endHour).coerceIn(0, 23)
                                endMinute = (parts[1].toIntOrNull() ?: endMinute).coerceIn(0, 59)
                            }
                        },
                        label = { Text("Until") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Days of week selector
                Text("Repeat Days:", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allDays) { day ->
                        val isSelected = selectedDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(day, fontSize = 11.sp) }
                        )
                    }
                }

                // Network checkboxes
                Text("Restrict On:", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = blockWifi,
                        onCheckedChange = { blockWifi = it }
                    )
                    Text("Block Wi-Fi")

                    Spacer(modifier = Modifier.width(16.dp))

                    Checkbox(
                        checked = blockMobile,
                        onCheckedChange = { blockMobile = it }
                    )
                    Text("Block Mobile Data")
                }

                OutlinedTextField(
                    value = targetPackages,
                    onValueChange = { targetPackages = it },
                    label = { Text("Target Apps") },
                    placeholder = { Text("Type ALL or comma-separated packages") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rule = ScheduleRule(
                        id = initialRule?.id ?: 0L,
                        name = name.ifBlank { "Scheduled Policy" },
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        daysOfWeek = selectedDays.joinToString(","),
                        targetPackageNames = targetPackages.ifBlank { "ALL" },
                        blockWifi = blockWifi,
                        blockMobile = blockMobile,
                        isEnabled = true
                    )
                    onSaveSchedule(rule)
                },
                modifier = Modifier.testTag("btn_save_schedule")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
