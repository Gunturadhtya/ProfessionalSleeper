package com.gntr.ui.quicknap

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QuickNapScreen(
    viewModel: QuickNapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (uiState.napState) {
            QuickNapState.IDLE -> {
                TimerSetupContent(
                    hours = uiState.selectedHours,
                    minutes = uiState.selectedMinutes,
                    seconds = uiState.selectedSeconds,
                    onHoursChanged = viewModel::onHoursChanged,
                    onMinutesChanged = viewModel::onMinutesChanged,
                    onSecondsChanged = viewModel::onSecondsChanged,
                    onStart = viewModel::startNap
                )
            }
            QuickNapState.RUNNING -> {
                CountdownContent(
                    remainingSeconds = uiState.remainingSeconds,
                    totalSeconds = uiState.totalSeconds,
                    onCancel = viewModel::cancelNap
                )
            }
        }
    }
}

@Composable
private fun TimerSetupContent(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChanged: (Int) -> Unit,
    onMinutesChanged: (Int) -> Unit,
    onSecondsChanged: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "Quick Nap",
            style = MaterialTheme.typography.headlineMedium.copy(
                letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrumPicker(
                value = hours,
                max = 23,
                label = "hr",
                onChange = onHoursChanged,
                modifier = Modifier.weight(1f)
            )
            TimeSeparator()
            DrumPicker(
                value = minutes,
                max = 59,
                label = "min",
                onChange = onMinutesChanged,
                modifier = Modifier.weight(1f)
            )
            TimeSeparator()
            DrumPicker(
                value = seconds,
                max = 59,
                label = "sec",
                onChange = onSecondsChanged,
                modifier = Modifier.weight(1f)
            )
        }

        StartButton(onClick = onStart)

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun TimeSeparator() {
    Text(
        text = ":",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun DrumPicker(
    value: Int,
    max: Int,
    label: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val range = max + 1
    val itemHeightPx = 72f

    val currentValue by rememberUpdatedState(newValue = value)

    var anchorValue by remember { mutableStateOf(0) }
    var totalDragPx by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .height(200.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        anchorValue = currentValue
                        totalDragPx = 0f
                    },
                    onDragEnd = { totalDragPx = 0f },
                    onDragCancel = { totalDragPx = 0f }
                ) { _, dragAmount ->
                    totalDragPx += dragAmount
                    val steps = (totalDragPx / itemHeightPx).toInt()
                    val newValue = ((anchorValue - steps) % range + range) % range
                    if (newValue != currentValue) onChange(newValue)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = ((value - 1 + range) % range).toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = ((value + 1) % range).toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = "START",
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing
        )
    }
}

@Composable
private fun CountdownContent(
    remainingSeconds: Long,
    totalSeconds: Long,
    onCancel: () -> Unit
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "napProgress"
    )

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "Napping...",
            style = MaterialTheme.typography.titleMedium.copy(
                letterSpacing = MaterialTheme.typography.titleMedium.letterSpacing
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(240.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                strokeWidth = 6.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val timeStr = if (hours > 0) {
                    "%d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    "%02d:%02d".format(minutes, seconds)
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = MaterialTheme.typography.displayMedium.letterSpacing
                )
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(
            onClick = onCancel,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(52.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancel", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(32.dp))
    }
}