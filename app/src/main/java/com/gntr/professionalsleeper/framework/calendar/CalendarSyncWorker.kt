package com.gntr.professionalsleeper.framework.calendar

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.professionalsleeper.data.local.dao.CalendarEventDao
import com.gntr.professionalsleeper.data.local.entity.CalendarEventEntity
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import com.gntr.professionalsleeper.domain.calendar.ICalendarSyncService
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import com.gntr.professionalsleeper.domain.repository.ITransactionRunner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarSyncService: ICalendarSyncService,
    private val repository: ISleepSessionRepository,
    private val transactionRunner: ITransactionRunner,
    private val alarmScheduler: IAlarmScheduler,
    private val calendarEventDao: CalendarEventDao,
    private val authManager: IAuthManager
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val account = authManager.getSignedInAccount()
        if (account == null) {
            Timber.w("Calendar sync aborted: No signed-in account found.")
            return Result.failure()
        }

        val accessToken = authManager.getCalendarAccessToken()
        if (accessToken == null) {
            Timber.w("Calendar sync aborted: Could not silently obtain a Calendar access token. Will retry.")
            return Result.retry()
        }

        val timeMin = System.currentTimeMillis()
        val timeMax = timeMin + (7L * 24 * 60 * 60 * 1000)

        return try {
            val eventsResult = calendarSyncService.fetchUpcomingEvents(accessToken, timeMin, timeMax)
            if (eventsResult.isSuccess) {
                val calendarEvents = eventsResult.getOrNull() ?: emptyList()

                val eventEntities = calendarEvents.map {
                    CalendarEventEntity(it.id, it.title, it.startTime, it.endTime)
                }

                val sessionsToReschedule = mutableListOf<SleepSession>()

                transactionRunner {
                    calendarEventDao.deleteAll()
                    calendarEventDao.insertAll(eventEntities)

                    val localSessions = repository.getSessionsSnapshotForTimeframe(timeMin, timeMax)

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
                                    .atZone(ZoneOffset.UTC)
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
                            repository.updateSession(resolvedSession)

                            sessionsToReschedule.add(resolvedSession)
                        }
                    }
                }

                sessionsToReschedule.forEach { session ->
                    alarmScheduler.scheduleAlarm(session)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Fatal exception occurred during background sync execution pass.")
            Result.retry()
        }
    }
}