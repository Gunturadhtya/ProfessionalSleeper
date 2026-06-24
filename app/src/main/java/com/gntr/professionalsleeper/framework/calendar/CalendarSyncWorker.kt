package com.gntr.professionalsleeper.framework.calendar

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.professionalsleeper.data.local.dao.CalendarEventDao
import com.gntr.professionalsleeper.data.local.dao.CalendarSourceDao
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
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarSyncService: ICalendarSyncService,
    private val repository: ISleepSessionRepository,
    private val transactionRunner: ITransactionRunner,
    private val alarmScheduler: IAlarmScheduler,
    private val calendarEventDao: CalendarEventDao,
    private val calendarSourceDao: CalendarSourceDao,
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

        val enabledSources = calendarSourceDao.getEnabledSources()
        if (enabledSources.isEmpty()) {
            Timber.i("Calendar sync skipped: No active calendar sources configured.")
            return Result.success()
        }

        val zoneId = ZoneId.systemDefault()
        val timeMin = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val timeMax = timeMin + (7L * 24 * 60 * 60 * 1000)

        val allAggregatedEvents = mutableListOf<CalendarEventEntity>()
        var partialFailure = false

        for (source in enabledSources) {
            try {
                val eventsResult = calendarSyncService.fetchUpcomingEvents(
                    accessToken,
                    source.id,
                    timeMin,
                    timeMax
                )

                if (eventsResult.isSuccess) {
                    val events = eventsResult.getOrNull() ?: emptyList()
                    allAggregatedEvents.addAll(events.map {
                        CalendarEventEntity(
                            id = it.id,
                            sourceId = source.id,
                            title = it.title,
                            startTime = it.startTime,
                            endTime = it.endTime
                        )
                    })
                } else {
                    Timber.w("Sync failed for calendar: ${source.displayName}")
                    partialFailure = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during sync for calendar: ${source.displayName}")
                partialFailure = true
            }
        }

        return try {
            val sessionsToReschedule = mutableListOf<SleepSession>()

            transactionRunner {
                val localEventIds = calendarEventDao.getEventIdsForTimeframe(timeMin, timeMax)
                val remoteEventIds = allAggregatedEvents.map { it.id }.toSet()

                val orphanedIds = localEventIds.filterNot { remoteEventIds.contains(it) }

                if (orphanedIds.isNotEmpty()) {
                    calendarEventDao.deleteEventsByIds(orphanedIds)
                }

                if (allAggregatedEvents.isNotEmpty()) {
                    calendarEventDao.insertAll(allAggregatedEvents)
                }

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

                        val conflictingEvent = allAggregatedEvents.firstOrNull { event ->
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
                        repository.updateSession(resolvedSession)
                        sessionsToReschedule.add(resolvedSession)
                    }
                }
            }

            sessionsToReschedule.forEach { session ->
                alarmScheduler.scheduleAlarm(session)
            }

            if (partialFailure) Result.retry() else Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Fatal exception occurred during database transaction or collision resolution.")
            Result.retry()
        }
    }
}