package com.gntr.professionalsleeper.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.dao.SleepDebtDao
import com.gntr.professionalsleeper.data.local.entity.SleepDebt
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import com.gntr.professionalsleeper.domain.repository.ITransactionRunner
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CalculateSleepDebtUseCase @Inject constructor(
    private val sessionRepository: ISleepSessionRepository,
    private val sleepDebtDao: SleepDebtDao,
    private val transactionRunner: ITransactionRunner
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(targetMinutesPerDay: Int = 330) {
        val today = LocalDate.now(ZoneId.systemDefault())
        val zoneId = ZoneId.systemDefault()

        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        transactionRunner {
            val sessionsToday = sessionRepository.getSessionsSnapshotForDay(startOfDay, endOfDay)

            val totalSleepMinutes = sessionsToday.sumOf { session ->
                Duration.between(session.startTime, session.endTime).toMinutes().toInt()
            }

            val debt = targetMinutesPerDay - totalSleepMinutes

            sleepDebtDao.upsertDebt(
                SleepDebt(
                    date = today.toString(),
                    debtMinutes = debt,
                    isSynced = false
                )
            )
        }
    }
}