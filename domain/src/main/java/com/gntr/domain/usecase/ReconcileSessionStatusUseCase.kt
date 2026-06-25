package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.service.SleepSessionManager
import java.time.ZonedDateTime
import javax.inject.Inject

class ReconcileSessionStatusUseCase @Inject constructor(
    private val repository: ISleepSessionRepository,
    private val sleepSessionManager: SleepSessionManager
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke() {
        val now = ZonedDateTime.now()
        val nowMilli = now.toInstant().toEpochMilli()

        val pastMilli = now.minusDays(7).toInstant().toEpochMilli()

        val snapshot = repository.getSessionsSnapshotForTimeframe(pastMilli, nowMilli)

        val staleSessions = snapshot.filter { session ->
            session.endTime.isBefore(now) &&
                    (session.status == SessionStatus.SCHEDULED || session.status == SessionStatus.BLOCKED_BY_EVENT)
        }

        for (staleSession in staleSessions) {
            val completedSession = staleSession.copy(status = SessionStatus.COMPLETED)
            sleepSessionManager.updateScheduled(completedSession)
        }
    }
}