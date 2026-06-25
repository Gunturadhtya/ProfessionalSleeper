package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.DailySleepMetrics
import com.gntr.domain.model.ScheduleTimeframe
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SleepAnalysisReport
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ISleepDebtRepository
import com.gntr.domain.repository.ISleepSessionRepository
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

class GenerateSleepReportUseCase @Inject constructor(
    private val sessionRepository: ISleepSessionRepository,
    private val sleepDebtRepository: ISleepDebtRepository
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(timeframe: ScheduleTimeframe): SleepAnalysisReport {
        val zoneId = ZoneId.systemDefault()

        val allSessions: List<SleepSession> =
            sessionRepository.getSessionsSnapshotForTimeframe(
                timeframe.startMilli,
                timeframe.endMilli
            )

        val startDate = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(timeframe.startMilli), zoneId
        )
        val endDate = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(timeframe.endMilli), zoneId
        )

        val debtByDate: Map<String, Int> =
            sleepDebtRepository
                .getDebtsForDateRange(startDate.toString(), endDate.toString())
                .associate { it.date to it.debtMinutes }

        val scheduledByDay: Map<LocalDate, List<SleepSession>> = allSessions
            .filter { it.status == SessionStatus.SCHEDULED }
            .groupBy { it.startTime.withZoneSameInstant(zoneId).toLocalDate() }

        val completedByDay: Map<LocalDate, List<SleepSession>> = allSessions
            .filter { it.status == SessionStatus.COMPLETED }
            .groupBy { it.startTime.withZoneSameInstant(zoneId).toLocalDate() }

        val dailyMetrics: List<DailySleepMetrics> = generateDateSequence(startDate, endDate)
            .map { day ->
                val scheduled = scheduledByDay[day].orEmpty()
                val completed = completedByDay[day].orEmpty()

                val totalScheduledMinutes = scheduled.sumOf { it.durationMinutes() }
                val totalActualMinutes = computeOverlapMinutes(scheduled, completed)

                val adherenceScore = when {
                    totalScheduledMinutes <= 0 -> 0f
                    else -> (totalActualMinutes.toFloat() / totalScheduledMinutes).coerceIn(0f, 1f)
                }

                DailySleepMetrics(
                    date = day,
                    totalScheduledMinutes = totalScheduledMinutes,
                    totalActualMinutes = totalActualMinutes,
                    adherenceScore = adherenceScore,
                    sleepDebtAccumulated = debtByDate[day.toString()] ?: 0
                )
            }
            .toList()

        return SleepAnalysisReport(dailyMetrics)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun SleepSession.durationMinutes(): Int =
        Duration.between(startTime, endTime).toMinutes().toInt().coerceAtLeast(0)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun computeOverlapMinutes(
        scheduled: List<SleepSession>,
        completed: List<SleepSession>
    ): Int {
        if (scheduled.isEmpty() || completed.isEmpty()) return 0

        var totalOverlapMinutes = 0
        for (s in scheduled) {
            for (c in completed) {
                val overlapStart: ZonedDateTime =
                    if (s.startTime.isAfter(c.startTime)) s.startTime else c.startTime
                val overlapEnd: ZonedDateTime =
                    if (s.endTime.isBefore(c.endTime)) s.endTime else c.endTime

                if (overlapEnd.isAfter(overlapStart)) {
                    totalOverlapMinutes +=
                        Duration.between(overlapStart, overlapEnd).toMinutes().toInt()
                }
            }
        }
        return totalOverlapMinutes
    }

    private fun generateDateSequence(start: LocalDate, endInclusive: LocalDate): Sequence<LocalDate> =
        generateSequence(start) { current ->
            val next = current.plusDays(1)
            if (next.isAfter(endInclusive)) null else next
        }
}