package com.gntr.domain.repository

import com.gntr.domain.model.SleepDebt

interface ISleepDebtRepository {
    suspend fun upsertDebt(sleepDebt: SleepDebt)
    suspend fun getUnsyncedDebts(): List<SleepDebt>
    suspend fun markAsSynced(dates: List<String>)
}