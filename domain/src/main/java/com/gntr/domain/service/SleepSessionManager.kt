package com.gntr.domain.service

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.alarm.IPreSessionNotificationScheduler
import com.gntr.domain.model.NotificationLeadTime
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ISleepSessionRepository
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepSessionManager @Inject constructor(
    private val repository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val preSessionNotificationScheduler: IPreSessionNotificationScheduler
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun scheduleNew(session: SleepSession): Long {
        val id = repository.insertSession(session)
        val sessionWithId = session.copy(id = id)

        if (sessionWithId.status == SessionStatus.SCHEDULED) {
            val now = ZonedDateTime.now()
            if (sessionWithId.endTime.isAfter(now)) {
                alarmScheduler.scheduleAlarm(sessionWithId)
            }
            if (sessionWithId.startTime.isAfter(now)) {
                val leadSeconds = NotificationLeadTime.forSession(session)
                preSessionNotificationScheduler.schedulePreSessionNotification(session, leadSeconds)
            }
        }
        return id
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateScheduled(session: SleepSession) {
        cancelAllSideEffects(session)
        repository.updateSession(session)

        if (session.status == SessionStatus.SCHEDULED) {
            val now = ZonedDateTime.now()
            if (session.endTime.isAfter(now)) {
                alarmScheduler.scheduleAlarm(session)
            }
            if (session.startTime.isAfter(now)) {
                val leadSeconds = NotificationLeadTime.forSession(session)
                preSessionNotificationScheduler.schedulePreSessionNotification(session, leadSeconds)
            }
        }
    }

    fun cancelAllSideEffects(session: SleepSession) {
        alarmScheduler.cancelAlarm(session)
        preSessionNotificationScheduler.cancelPreSessionNotification(session)
    }

    suspend fun clearAllAndCancelSideEffects() {
        val deletedSessions = repository.clearAllSessions()
        deletedSessions.forEach { cancelAllSideEffects(it) }
    }

    suspend fun cancelSession(session: SleepSession) {
        cancelAllSideEffects(session)

        val cancelledSession = session.copy(status = SessionStatus.CANCELLED)

        repository.updateSession(cancelledSession)
    }
}