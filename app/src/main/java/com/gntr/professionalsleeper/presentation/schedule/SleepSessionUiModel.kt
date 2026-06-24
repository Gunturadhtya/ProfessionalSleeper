package com.gntr.professionalsleeper.presentation.schedule

import androidx.compose.ui.graphics.Color
import com.gntr.professionalsleeper.domain.model.SessionStatus
import com.gntr.professionalsleeper.domain.model.SessionType

data class SleepSessionUiModel(
    val id: Long,
    val timeRange: String,
    val durationText: String,
    val typeLabel: String,
    val accentColor: Color,
    val statusLabel: String,
    val statusBgColor: Color,
    val statusTextColor: Color,
    val type: SessionType,
    val status: SessionStatus,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)