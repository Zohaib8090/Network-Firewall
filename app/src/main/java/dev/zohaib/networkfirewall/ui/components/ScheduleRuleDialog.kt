package dev.zohaib.networkfirewall.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zohaib.networkfirewall.data.model.ScheduleRule

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
    var pickingStart by remember { mutableStateOf<Boolean?>(null) } // null = closed, true = start, false = end

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
                    TimeField(
                        label = "From",
                        hour = startHour,
                        minute = startMinute,
                        onClick = { pickingStart = true },
                        modifier = Modifier.weight(1f).testTag("input_schedule_start")
                    )

                    Text("to", fontWeight = FontWeight.Bold)

                    TimeField(
                        label = "Until",
                        hour = endHour,
                        minute = endMinute,
                        onClick = { pickingStart = false },
                        modifier = Modifier.weight(1f).testTag("input_schedule_end")
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

    pickingStart?.let { editingStart ->
        TimePickerDialog(
            title = if (editingStart) "Start time" else "End time",
            initialHour = if (editingStart) startHour else endHour,
            initialMinute = if (editingStart) startMinute else endMinute,
            onConfirm = { hour, minute ->
                if (editingStart) {
                    startHour = hour
                    startMinute = minute
                } else {
                    endHour = hour
                    endMinute = minute
                }
                pickingStart = null
            },
            onDismiss = { pickingStart = null }
        )
    }
}

/** Read-only field that opens a time picker when tapped, so a time can never be half-typed. */
@Composable
internal fun TimeField(
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = String.format("%02d:%02d", hour, minute),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay: a read-only text field would otherwise swallow the tap
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClickLabel = "Change $label time", role = Role.Button, onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
