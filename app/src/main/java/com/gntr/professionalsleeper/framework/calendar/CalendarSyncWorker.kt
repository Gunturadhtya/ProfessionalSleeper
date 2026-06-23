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
                    val startMilli = session.startTime.toInstant().toEpochMilli()
                    val endMilli = session.endTime.toInstant().toEpochMilli()

                    val hasConflict = calendarSyncService.checkScheduleConflict(startMilli, endMilli, calendarEvents)

                    if (hasConflict && session.status != SessionStatus.COMPLETED) {
                        val resolvedSession = session.copy(status = SessionStatus.SCHEDULED)
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