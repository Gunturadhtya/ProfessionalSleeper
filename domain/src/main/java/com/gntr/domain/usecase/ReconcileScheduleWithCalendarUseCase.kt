package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.EventTime
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SleepSession
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ReconcileScheduleWithCalendarUseCase @Inject constructor() {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(
        localSessions: List<SleepSession>,
        calendarEvents: List<CalendarEvent>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<SleepSession> {
        Timber.d("--- STARTING SCHEDULE RECONCILIATION ---")
        Timber.d("Local Sessions provided: %d, Calendar Events provided: %d", localSessions.size, calendarEvents.size)

        val sessionsToUpdate = mutableListOf<SleepSession>()

        for (session in localSessions) {
            Timber.d("Evaluating Session ID: %s | Current Status: %s | Start: %s | End: %s", session.id, session.status, session.startTime, session.endTime)

            if (session.status == SessionStatus.CANCELLED || session.status == SessionStatus.COMPLETED) {
                Timber.d("-> Skipping Session ID: %s (Status is %s)", session.id, session.status)
                continue
            }

            var newStart = session.startTime
            var newEnd = session.endTime
            val duration = Duration.between(newStart, newEnd)

            val busyBlocks = calendarEvents.map { event ->
                val start = when (val s = event.startTime) {
                    is EventTime.Exact -> s.epochMilli
                    is EventTime.AllDay -> s.date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                }
                val end = when (val e = event.endTime) {
                    is EventTime.Exact -> e.epochMilli
                    is EventTime.AllDay -> e.date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                }
                Pair(start, end)
            } + localSessions.filter { it.id != session.id && it.status == SessionStatus.SCHEDULED }.map {
                Pair(it.startTime.toInstant().toEpochMilli(), it.endTime.toInstant().toEpochMilli())
            }

            Timber.d("-> Total Busy Blocks constructed for this session: %d", busyBlocks.size)

            var iterations = 0
            var hasCollision = false

            while (iterations < 100) {
                val currentStartMillis = newStart.toInstant().toEpochMilli()
                val currentEndMillis = newEnd.toInstant().toEpochMilli()

                val conflict = busyBlocks.find { block ->
                    currentStartMillis < block.second && currentEndMillis > block.first
                }

                if (conflict != null) {
                    hasCollision = true
                    val conflictStartZDT = Instant.ofEpochMilli(conflict.first).atZone(zoneId)
                    val conflictEndZDT = Instant.ofEpochMilli(conflict.second).atZone(zoneId)

                    Timber.d("   [Iteration %d] COLLISION DETECTED! Overlaps with block [%s -> %s]", iterations, conflictStartZDT, conflictEndZDT)

                    newStart = Instant.ofEpochMilli(conflict.second).atZone(zoneId)
                    newEnd = newStart.plus(duration)

                    Timber.d("   -> Bumping session forward. New temporary Start: %s", newStart)
                    iterations++
                } else {
                    Timber.d("   -> Clear gap found! Loop finished after %d iterations.", iterations)
                    break
                }
            }

            val shiftedHours = Duration.between(session.startTime, newStart).toHours()
            Timber.d("-> Session shift calculated: %d hours.", shiftedHours)

            if (shiftedHours < 24) {
                if (newStart != session.startTime || session.status == SessionStatus.BLOCKED_BY_EVENT) {
                    Timber.d("-> DECISION: Rescheduling Session ID: %s to new Start: %s (Status: SCHEDULED)", session.id, newStart)
                    sessionsToUpdate.add(
                        session.copy(
                            startTime = newStart,
                            endTime = newEnd,
                            status = SessionStatus.SCHEDULED
                        )
                    )
                } else {
                    Timber.d("-> DECISION: No changes needed for Session ID: %s", session.id)
                }
            } else {
                Timber.d("-> DECISION: Session ID: %s pushed more than 24 hours. Falling back to BLOCKED_BY_EVENT.", session.id)
                if (session.status == SessionStatus.SCHEDULED) {
                    sessionsToUpdate.add(session.copy(status = SessionStatus.BLOCKED_BY_EVENT))
                }
            }
        }

        Timber.d("--- RECONCILIATION COMPLETE. Updating %d sessions ---", sessionsToUpdate.size)
        return sessionsToUpdate
    }
}