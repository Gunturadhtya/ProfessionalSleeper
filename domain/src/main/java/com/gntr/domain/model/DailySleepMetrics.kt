package com.gntr.domain.model

import java.time.LocalDate

data class DailySleepMetrics(
    val date: LocalDate,
    val totalScheduledMinutes: Int,
    val totalActualMinutes: Int,
    val adherenceScore: Float,
    val sleepDebtAccumulated: Int
)