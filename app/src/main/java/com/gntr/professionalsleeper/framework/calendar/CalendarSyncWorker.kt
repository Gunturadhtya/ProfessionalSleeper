import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
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
    private val alarmScheduler: IAlarmScheduler
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val accountEmail = inputData.getString("account_email") ?: return Result.failure()
        val timeMin = System.currentTimeMillis()
        val timeMax = timeMin + (7L * 24 * 60 * 60 * 1000)

        return try {
            val eventsResult = calendarSyncService.fetchUpcomingEvents(accountEmail, timeMin, timeMax)
            if (eventsResult.isFailure) return Result.retry()

            val calendarEvents = eventsResult.getOrNull() ?: emptyList()
            val sessionsToReschedule = mutableListOf<SleepSession>()

            transactionRunner {
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
                            proposedStart = Instant.ofEpochMilli(conflictingEvent.endTime).atZone(ZoneOffset.UTC)
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
        } catch (e: Exception) {
            Timber.e(e, "Fatal exception occurred during background sync execution pass.")
            Result.retry()
        }
    }
}