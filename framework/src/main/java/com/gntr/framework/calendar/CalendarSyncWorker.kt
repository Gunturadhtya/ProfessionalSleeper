package com.gntr.framework.calendar

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.auth.IAuthManager
import com.gntr.domain.calendar.ICalendarSyncService
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.domain.repository.ICalendarSourceRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.repository.ITransactionRunner
import com.gntr.domain.service.SleepSessionManager
import com.gntr.domain.usecase.ReconcileScheduleWithCalendarUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
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
    private val calendarEventRepository: ICalendarEventRepository,
    private val calendarSourceRepository: ICalendarSourceRepository,
    private val authManager: IAuthManager,
    private val reconcileScheduleUseCase: ReconcileScheduleWithCalendarUseCase,
    private val sleepSessionManager: SleepSessionManager
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

        val enabledSources = calendarSourceRepository.getEnabledSources()
        if (enabledSources.isEmpty()) {
            Timber.i("Calendar sync skipped: No active calendar sources configured.")
            return Result.success()
        }

        val zoneId = ZoneId.systemDefault()
        val timeMin = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val timeMax = timeMin + (7L * 24 * 60 * 60 * 1000)

        val domainCalendarEvents = mutableListOf<CalendarEvent>()
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
                    domainCalendarEvents.addAll(events)
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
            var sessionsToReschedule = emptyList<SleepSession>()

            transactionRunner {
                val localEventIds = calendarEventRepository.getEventIdsForTimeframe(timeMin, timeMax)
                val remoteEventIds = domainCalendarEvents.map { it.id }.toSet()

                val orphanedIds = localEventIds.filterNot { remoteEventIds.contains(it) }

                if (orphanedIds.isNotEmpty()) {
                    calendarEventRepository.deleteEventsByIds(orphanedIds)
                }

                if (domainCalendarEvents.isNotEmpty()) {
                    calendarEventRepository.insertAll(domainCalendarEvents)
                }

                val localSessions = repository.getSessionsSnapshotForTimeframe(timeMin, timeMax)
                sessionsToReschedule = reconcileScheduleUseCase(localSessions, domainCalendarEvents)

                sessionsToReschedule.forEach { resolvedSession ->
                    repository.updateSession(resolvedSession)
                }
            }

            sessionsToReschedule.forEach { resolvedSession ->
                sleepSessionManager.updateScheduled(resolvedSession)
            }

            if (partialFailure) Result.retry() else Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Fatal exception occurred during database transaction or collision resolution.")
            Result.retry()
        }
    }
}