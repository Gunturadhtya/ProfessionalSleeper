package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepDebt
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ISleepDebtRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.repository.ITransactionRunner
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

class SeedDatabaseUseCase @Inject constructor(
    private val sessionRepository: ISleepSessionRepository,
    private val sleepDebtRepository: ISleepDebtRepository,
    private val transactionRunner: ITransactionRunner
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke() {
        transactionRunner {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val targetMinutes = 380

            for (i in 30 downTo 1) {
                val date = today.minusDays(i.toLong())

                val adherence = Random.nextDouble(0.65, 1.0)
                val coreDuration = (360 * adherence).toLong()
                val lateMinutes = Random.nextLong(0, 45)

                val coreStart = date.minusDays(1).atTime(23, 0).atZone(zoneId)
                val coreEnd = coreStart.plusMinutes(360)

                sessionRepository.insertSession(
                    SleepSession(
                        startTime = coreStart,
                        endTime = coreEnd,
                        type = SessionType.CORE,
                        status = SessionStatus.SCHEDULED
                    )
                )

                sessionRepository.insertSession(
                    SleepSession(
                        startTime = coreStart.plusMinutes(lateMinutes),
                        endTime = coreStart.plusMinutes(lateMinutes + coreDuration),
                        type = SessionType.CORE,
                        status = SessionStatus.COMPLETED
                    )
                )

                val napStart = date.atTime(14, 0).atZone(zoneId)

                sessionRepository.insertSession(
                    SleepSession(
                        startTime = napStart,
                        endTime = napStart.plusMinutes(20),
                        type = SessionType.NAP,
                        status = SessionStatus.SCHEDULED
                    )
                )

                val tookNap = Random.nextDouble() > 0.2
                if (tookNap) {
                    sessionRepository.insertSession(
                        SleepSession(
                            startTime = napStart,
                            endTime = napStart.plusMinutes(20),
                            type = SessionType.NAP,
                            status = SessionStatus.COMPLETED
                        )
                    )
                }

                val actualMinutes = coreDuration + (if (tookNap) 20 else 0)
                val debt = targetMinutes - actualMinutes.toInt()

                sleepDebtRepository.upsertDebt(
                    SleepDebt(
                        date = date.toString(),
                        debtMinutes = debt,
                        isSynced = false
                    )
                )
            }
        }
    }
}