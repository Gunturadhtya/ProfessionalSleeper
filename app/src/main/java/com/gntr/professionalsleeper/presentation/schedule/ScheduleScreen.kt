package com.gntr.professionalsleeper.presentation.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.presentation.MainViewModel
import com.gntr.professionalsleeper.presentation.schedule.sectograph.Sectograph
import com.gntr.professionalsleeper.ui.theme.CoreSleepColor
import com.gntr.professionalsleeper.ui.theme.JetBrainsMono
import com.gntr.professionalsleeper.ui.theme.NapSleepColor
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val sessions by viewModel.todaySessions.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val sleepSectors by viewModel.sleepSectors.collectAsStateWithLifecycle()

    val calendarSectors = emptyList<com.gntr.professionalsleeper.presentation.schedule.sectograph.SectographSector>()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val pullToRefreshState = rememberPullToRefreshState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        sheetContent = { SleepAnalysisSheetContent() },
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
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.triggerCalendarSync() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Sectograph(
                        sleepSectors = sleepSectors,
                        calendarSectors = calendarSectors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }

                items(sessions, key = { it.id }) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
private fun SleepAnalysisSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sleep Analysis",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Historical Data Visualization...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
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