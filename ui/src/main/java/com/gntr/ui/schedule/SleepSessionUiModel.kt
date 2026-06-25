package com.gntr.ui.schedule

import androidx.compose.ui.graphics.Color
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType

data class SleepSessionUiModel(
    val id: Long,
    val timeRange: String,
    val durationText: String,
    val typeLabel: String,
    val type: SessionType,
    val status: SessionStatus,
    val isOngoing: Boolean,
    val isPast: Boolean,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)