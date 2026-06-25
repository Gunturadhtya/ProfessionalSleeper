package com.gntr.data.repository

import com.gntr.data.local.dao.SleepDebtDao
import com.gntr.data.local.mapper.toDomain
import com.gntr.data.local.mapper.toEntity
import com.gntr.domain.model.SleepDebt
import com.gntr.domain.repository.ISleepDebtRepository
import javax.inject.Inject

class SleepDebtRepositoryImpl @Inject constructor(
    private val dao: SleepDebtDao
) : ISleepDebtRepository {

    override suspend fun upsertDebt(sleepDebt: SleepDebt) {
        dao.upsertDebt(sleepDebt.toEntity())
    }

    override suspend fun getUnsyncedDebts(): List<SleepDebt> {
        return dao.getUnsyncedDebts().map { it.toDomain() }
    }

    override suspend fun markAsSynced(dates: List<String>) {
        dao.markAsSynced(dates)
    }

    override suspend fun getDebtsForDateRange(
        startDateInclusive: String,
        endDateInclusive: String
    ): List<SleepDebt> {
        return dao.getDebtsForDateRange(startDateInclusive, endDateInclusive).map { it.toDomain() }
    }
}