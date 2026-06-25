package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SleepSession
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ReconcileScheduleWithCalendarUseCase @Inject constructor() {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(
        localSessions: List<SleepSession>,
        calendarEvents: List<CalendarEvent>
    ): List<SleepSession> {
        val sessionsToReschedule = mutableListOf<SleepSession>()

        for (session in localSessions) {
            if (session.status == SessionStatus.COMPLETED) continue

            var proposedStart = session.startTime
            var proposedEnd = session.endTime
            val sessionDuration = Duration.between(proposedStart, proposedEnd)

            var hasConflict = true
            var boundsMutated = false

            while (hasConflict) {
                val startMilli = proposedStart.toInstant().toEpochMilli()
                val endMilli = proposedEnd.toInstant().toEpochMilli()

                val conflictingEvent = calendarEvents.firstOrNull { event ->
                    startMilli < event.endTime && endMilli > event.startTime
                }

                if (conflictingEvent != null) {
                    proposedStart = Instant.ofEpochMilli(conflictingEvent.endTime)
                        .atZone(ZoneId.systemDefault())
                    proposedEnd = proposedStart.plus(sessionDuration)
                    boundsMutated = true
                } else {
                    hasConflict = false
                }
            }

            if (boundsMutated) {
                val resolvedSession = session.copy(
                    startTime = proposedStart,
                    endTime = proposedEnd,
                    status = SessionStatus.SCHEDULED
                )
                sessionsToReschedule.add(resolvedSession)
            }
        }

        return sessionsToReschedule
    }
}