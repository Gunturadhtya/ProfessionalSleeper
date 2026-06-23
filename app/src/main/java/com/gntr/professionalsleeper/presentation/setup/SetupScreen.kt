package com.gntr.professionalsleeper.presentation.setup

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gntr.professionalsleeper.data.local.entity.EverymanType
import java.time.LocalTime
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    var selectedEveryman by remember { mutableStateOf<EverymanType?>(null) }
    var wakeUpTime by remember { mutableStateOf(LocalTime.of(7, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Pilih tipe Everyman:", style = MaterialTheme.typography.headlineSmall)

        EverymanType.entries.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { selectedEveryman = type }
            ) {
                RadioButton(selected = (selectedEveryman == type), onClick = { selectedEveryman = type })
                Text("${type.displayName} (Core: ${type.coreSleepMinutes}m, Nap: ${type.napCount}x)")
            }
        }

        Button(onClick = { showTimePicker = true }) {
            Text("Pilih Jam Bangun: ${wakeUpTime}")
        }

        Button(
            onClick = {
                selectedEveryman?.let { type ->
                    val zdt = ZonedDateTime.now().with(wakeUpTime)
                    viewModel.completeSetup(type, zdt)
                    onComplete()
                }
            },
            enabled = selectedEveryman != null
        ) {
            Text("Simpan & Mulai")
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = wakeUpTime.hour,
                initialMinute = wakeUpTime.minute,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = {
                    Text("Pilih Jam Bangun")
                },
                text = {
                    TimePicker(
                        state = timePickerState
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            wakeUpTime = LocalTime.of(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            showTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showTimePicker = false
                        }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}