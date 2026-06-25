package com.gntr.ui.schedule.edit

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gntr.ui.R
import com.gntr.domain.model.SessionType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionScreen(
    viewModel: EditSessionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.saveEvents.collect {
            onNavigateBack()
        }
    }

    var editingField by remember { mutableStateOf<TimeField?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_session_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SleepTypeSelector(
                selectedType = uiState.type,
                onTypeSelected = viewModel::onTypeChanged
            )

            TimeField(
                label = stringResource(R.string.edit_session_start_time),
                timeMillis = uiState.startTimeMillis,
                onClick = { editingField = TimeField.START }
            )

            TimeField(
                label = stringResource(R.string.edit_session_end_time),
                timeMillis = uiState.endTimeMillis,
                onClick = { editingField = TimeField.END }
            )

            DurationMinutesField(
                minutes = uiState.durationMinutes,
                onMinutesChanged = viewModel::onDurationMinutesChanged
            )

            Button(
                onClick = { viewModel.saveSession() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.endTimeMillis > uiState.startTimeMillis
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }

    editingField?.let { field ->
        val initialMillis = if (field == TimeField.START) uiState.startTimeMillis else uiState.endTimeMillis
        TimePickerDialog(
            initialMillis = initialMillis,
            title = if (field == TimeField.START) {
                stringResource(R.string.edit_session_pick_start_time)
            } else {
                stringResource(R.string.edit_session_pick_end_time)
            },
            onConfirm = { newMillis ->
                if (field == TimeField.START) {
                    viewModel.onStartTimeChanged(newMillis)
                } else {
                    viewModel.onEndTimeChanged(newMillis)
                }
                editingField = null
            },
            onDismiss = { editingField = null }
        )
    }
}

private enum class TimeField { START, END }

@Composable
private fun SleepTypeSelector(
    selectedType: SessionType,
    onTypeSelected: (SessionType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.edit_session_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionType.entries.forEach { type ->
                // Route session type to the correct semantic colorScheme role
                val accent = if (type == SessionType.CORE)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.tertiary

                val label = if (type == SessionType.CORE) {
                    stringResource(R.string.session_type_core)
                } else {
                    stringResource(R.string.session_type_nap)
                }
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.15f),
                        selectedLabelColor = accent
                    )
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimeField(
    label: String,
    timeMillis: Long,
    onClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val timeText = remember(timeMillis) {
        Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).format(formatter)
    }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DurationMinutesField(
    minutes: Long,
    onMinutesChanged: (Long) -> Unit
) {
    var text by remember(minutes) { mutableStateOf(minutes.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            newValue.toLongOrNull()?.let { onMinutesChanged(it) }
        },
        label = { Text(stringResource(R.string.edit_session_duration_minutes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMillis: Long,
    title: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialZdt = remember(initialMillis) {
        Instant.ofEpochMilli(initialMillis).atZone(ZoneId.systemDefault())
    }

    val timePickerState = rememberTimePickerState(
        initialHour = initialZdt.hour,
        initialMinute = initialZdt.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(onClick = {
                val newZdt = initialZdt.with(LocalTime.of(timePickerState.hour, timePickerState.minute))
                onConfirm(newZdt.toInstant().toEpochMilli())
            }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}