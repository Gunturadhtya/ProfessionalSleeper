package com.gntr.ui.schedule.edit

import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType

data class EditSessionUiState(
    val isLoading: Boolean = true,
    val sessionId: Long = -1L,
    val type: SessionType = SessionType.NAP,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
) {
    val durationMinutes: Long
        get() = ((endTimeMillis - startTimeMillis) / 60_000L).coerceAtLeast(0L)
}