package com.gntr.professionalsleeper.framework.calendar

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.domain.calendar.ICalendarSyncService
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.ZoneId

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarSyncService: ICalendarSyncService,
    private val repository: ISleepSessionRepository
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        Timber.d("Executing periodic background calendar synchronization engine.")
        val accountEmail = inputData.getString("account_email") ?: return Result.failure()

        val timeMin = System.currentTimeMillis()
        val timeMax = timeMin + (7L * 24 * 60 * 60 * 1000)

        return try {
            val eventsResult = calendarSyncService.fetchUpcomingEvents(accountEmail, timeMin, timeMax)

            if (eventsResult.isSuccess) {
                val calendarEvents = eventsResult.getOrNull() ?: emptyList()
                Timber.i("Successfully retrieved ${calendarEvents.size} external calendar events.")

                val localSessions = repository.getSessionsForToday().first()

                for (session in localSessions) {
                    val startMilli = session.startTime.toInstant().toEpochMilli()
                    val endMilli = session.endTime.toInstant().toEpochMilli()

                    val hasConflict = calendarSyncService.checkScheduleConflict(
                        napStartTime = startMilli,
                        napEndTime = endMilli,
                        events = calendarEvents
                    )

                    if (hasConflict && session.status != SessionStatus.COMPLETED) {
                        Timber.w("Conflict detected for sleep session ID: ${session.id}. Triggering dynamic adjustment.")

                        val resolvedSession = session.copy(
                            status = SessionStatus.SCHEDULED
                        )
                        repository.updateSession(resolvedSession)
                    }
                }
                Result.success()
            } else {
                val exception = eventsResult.exceptionOrNull()
                Timber.e(exception, "Google Calendar API execution failed.")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Fatal exception occurred during background sync execution pass.")
            Result.retry()
        }
    }
}