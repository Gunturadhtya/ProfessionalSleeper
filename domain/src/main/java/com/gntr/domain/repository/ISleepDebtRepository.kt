package com.gntr.domain.repository

import com.gntr.domain.model.SleepDebt

interface ISleepDebtRepository {
    suspend fun upsertDebt(sleepDebt: SleepDebt)
    suspend fun getDebtsForDateRange(
        startDateInclusive: String,
        endDateInclusive: String
    ): List<SleepDebt>
}