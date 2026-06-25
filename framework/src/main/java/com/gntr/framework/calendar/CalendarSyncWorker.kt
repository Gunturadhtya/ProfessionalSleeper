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
import com.gntr.domain.calendar.SyncFailure
import com.gntr.domain.calendar.SyncResult
import com.gntr.domain.calendar.SyncSuccess
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.domain.repository.ICalendarSourceRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.repository.ITransactionRunner
import com.gntr.domain.service.SleepSessionManager
import com.gntr.domain.usecase.BuildCalendarSyncPlanUseCase
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
    private val sleepSessionManager: SleepSessionManager,
    private val buildCalendarSyncPlanUseCase: BuildCalendarSyncPlanUseCase
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

        val resultsBySourceId = mutableMapOf<String, SyncResult<List<CalendarEvent>>>()
        for (source in enabledSources) {
            resultsBySourceId[source.id] = calendarSyncService.fetchUpcomingEvents(accessToken, source.id, timeMin, timeMax)
        }

        val localIdsBySourceId = mutableMapOf<String, List<String>>()
        for (source in enabledSources) {
            localIdsBySourceId[source.id] = calendarEventRepository.getEventIdsForSourceAndTimeframe(source.id, timeMin, timeMax)
        }

        val plan = buildCalendarSyncPlanUseCase(resultsBySourceId, localIdsBySourceId)

        return try {
            var sessionsToReschedule = emptyList<SleepSession>()

            transactionRunner {
                if (plan.eventIdsToDelete.isNotEmpty()) {
                    calendarEventRepository.deleteEventsByIds(plan.eventIdsToDelete)
                }
                if (plan.eventsToInsert.isNotEmpty()) {
                    calendarEventRepository.insertAll(plan.eventsToInsert)
                }
                for (sourceId in plan.sourceIdsToDisable) {
                    calendarSourceRepository.disableSource(sourceId)
                }

                val transientFailedSourceIds = resultsBySourceId
                    .filterValues { it is SyncFailure && it.error is SyncError.Transient }
                    .keys.toList()

                val allEventsForReconciliation = plan.eventsToInsert +
                        if (transientFailedSourceIds.isNotEmpty()) {
                            calendarEventRepository.getEventsForSourcesSnapshot(transientFailedSourceIds, timeMin, timeMax)
                        } else emptyList()

                val localSessions = repository.getSessionsSnapshotForTimeframe(timeMin, timeMax)
                sessionsToReschedule = reconcileScheduleUseCase(localSessions, allEventsForReconciliation)
                for (session in sessionsToReschedule) {
                    repository.updateSession(session)
                }
            }

            for (session in sessionsToReschedule) {
                sleepSessionManager.updateScheduled(session)
            }

            if (plan.requiresRetry) Result.retry() else Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Fatal database transaction exception during synchronization.")
            Result.retry()
        }
    }
}