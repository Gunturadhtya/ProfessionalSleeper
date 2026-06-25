package com.gntr.ui.quicknap

data class QuickNapUiState(
    val selectedHours: Int = 0,
    val selectedMinutes: Int = 20,
    val selectedSeconds: Int = 0,
    val remainingSeconds: Long = 0L,
    val napState: QuickNapState = QuickNapState.IDLE,
    val totalSeconds: Long = 0L
)
