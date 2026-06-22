package com.gntr.professionalsleeper.presentation.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.presentation.MainViewModel
import com.gntr.professionalsleeper.ui.theme.CoreSleepColor
import com.gntr.professionalsleeper.ui.theme.JetBrainsMono
import com.gntr.professionalsleeper.ui.theme.NapSleepColor
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val sessions by viewModel.todaySessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Circadian Precision", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { viewModel.triggerDebugAlarm() }) {
                        Icon(Icons.Default.Warning, contentDescription = "Debug Alarm", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    viewModel.scheduleNewSession(
                        startTime = now,
                        endTime = now + (20 * 60 * 1000), // hardcoded
                        type = SessionType.NAP
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Nap", modifier = Modifier.size(36.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionCard(session)
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SessionCard(session: SleepSession) {
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val startStr = session.startTime.format(timeFormat)
    val endStr = session.endTime.format(timeFormat)

    val accentColor = if (session.type == SessionType.CORE) CoreSleepColor else NapSleepColor

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = session.type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$startStr - $endStr",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = JetBrainsMono
                        )
                    )
                }
            }

            Box(modifier = Modifier.padding(16.dp).align(Alignment.CenterVertically)) {
                Text(
                    text = session.status.name,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}