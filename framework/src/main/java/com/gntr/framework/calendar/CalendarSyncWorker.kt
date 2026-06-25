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
import com.gntr.domain.calendar.SyncError
import com.gntr.domain.calendar.SyncResult
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.domain.repository.ICalendarSourceRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.repository.ITransactionRunner
import com.gntr.domain.service.SleepSessionManager
import com.gntr.domain.usecase.ReconcileCalendarsUseCase
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
    private val reconcileCalendarsUseCase: ReconcileCalendarsUseCase,
    private val sleepSessionManager: SleepSessionManager
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val account = authManager.getSignedInAccount() ?: return Result.failure()
        val accessToken = authManager.getCalendarAccessToken() ?: return Result.retry()
        var enabledSources = calendarSourceRepository.getEnabledSources()

        if (enabledSources.isEmpty()) {
            reconcileCalendarsUseCase(accessToken)
            enabledSources = calendarSourceRepository.getEnabledSources()
            if (enabledSources.isEmpty()) return Result.success()
        }

        val zoneId = ZoneId.systemDefault()
        val timeMin = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val timeMax = timeMin + (7L * 24 * 60 * 60 * 1000)

        val successfulSources = mutableListOf<String>()
        val transientFailedSources = mutableListOf<String>()
        val permanentlyRevokedSources = mutableListOf<String>()
        val fetchedCalendarEvents = mutableListOf<CalendarEvent>()
        var requiresRetry = false

        for (source in enabledSources) {
            when (val result = calendarSyncService.fetchUpcomingEvents(accessToken, source.id, timeMin, timeMax)) {
                is SyncResult.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val events = result.data as List<CalendarEvent>

                    fetchedCalendarEvents.addAll(events)
                    successfulSources.add(source.id)
                }
                is SyncResult.Failure -> {
                    when (val error = result.error) {
                        is SyncError.Transient -> {
                            Timber.w(error.exception, "Transient network failure for source: ${source.id}")
                            transientFailedSources.add(source.id)
                            requiresRetry = true
                        }
                        is SyncError.PermanentAuthFailure -> {
                            Timber.e("Permanent authorization failure for source: ${source.id}. Code: ${error.statusCode}. Revoking.")
                            permanentlyRevokedSources.add(source.id)
                            calendarSourceRepository.disableSource(source.id)
                        }
                    }
                }
            }
        }

        return try {
            var sessionsToReschedule = emptyList<SleepSession>()

            transactionRunner {
                val orphanedIdsToClear = mutableListOf<String>()

                for (sourceId in successfulSources) {
                    val localEventIds = calendarEventRepository.getEventIdsForSourceAndTimeframe(sourceId, timeMin, timeMax)
                    val remoteEventIds = fetchedCalendarEvents.filter { it.sourceId == sourceId }.map { it.id }.toSet()
                    orphanedIdsToClear.addAll(localEventIds.filterNot { remoteEventIds.contains(it) })
                }

                for (sourceId in permanentlyRevokedSources) {
                    val allLocalEventIdsForSource = calendarEventRepository.getEventIdsForSourceAndTimeframe(sourceId, timeMin, timeMax)
                    orphanedIdsToClear.addAll(allLocalEventIdsForSource)
                }

                if (orphanedIdsToClear.isNotEmpty()) {
                    calendarEventRepository.deleteEventsByIds(orphanedIdsToClear)
                }

                if (fetchedCalendarEvents.isNotEmpty()) {
                    calendarEventRepository.insertAll(fetchedCalendarEvents)
                }

                val allEventsForReconciliation = mutableListOf<CalendarEvent>()
                allEventsForReconciliation.addAll(fetchedCalendarEvents)

                if (transientFailedSources.isNotEmpty()) {
                    val retainedEvents = calendarEventRepository.getEventsForSourcesSnapshot(transientFailedSources, timeMin, timeMax)
                    allEventsForReconciliation.addAll(retainedEvents)
                }

                val localSessions = repository.getSessionsSnapshotForTimeframe(timeMin, timeMax)

                sessionsToReschedule = reconcileScheduleUseCase(localSessions, allEventsForReconciliation)

                sessionsToReschedule.forEach { resolvedSession ->
                    repository.updateSession(resolvedSession)
                }
            }

            sessionsToReschedule.forEach { resolvedSession ->
                sleepSessionManager.updateScheduled(resolvedSession)
            }

            if (requiresRetry) Result.retry() else Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Fatal database transaction exception during synchronization.")
            Result.retry()
        }
    }
}