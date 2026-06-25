package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SleepSession
import javax.inject.Inject

class ReconcileScheduleWithCalendarUseCase @Inject constructor() {
    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(
        localSessions: List<SleepSession>,
        events: List<CalendarEvent>
    ): List<SleepSession> {
        val sessionsToReschedule = mutableListOf<SleepSession>()

        for (session in localSessions) {
            if (session.status != SessionStatus.SCHEDULED) continue

            val sessionStart = session.startTime.toInstant().toEpochMilli()
            val sessionEnd = session.endTime.toInstant().toEpochMilli()

            val hasCollision = events.any { event ->
                event.startTime < sessionEnd && event.endTime > sessionStart
            }

            if (hasCollision) {
                sessionsToReschedule.add(session.copy(status = SessionStatus.CANCELLED))
            }
        }
        return sessionsToReschedule
    }
}