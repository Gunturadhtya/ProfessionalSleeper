package com.gntr.professionalsleeper.presentation.schedule

import androidx.compose.ui.graphics.Color

data class SleepSessionUiModel(
    val id: Int,
    val timeRange: String,
    val durationText: String,
    val typeLabel: String,
    val accentColor: Color,
    val statusLabel: String,
    val statusBgColor: Color,
    val statusTextColor: Color
)