package com.gntr.professionalsleeper.presentation

import android.content.Context
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gntr.professionalsleeper.data.local.dao.CalendarEventDao
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import com.gntr.professionalsleeper.framework.calendar.CalendarSyncWorker
import com.gntr.professionalsleeper.framework.launcher.AppInfo
import com.gntr.professionalsleeper.framework.launcher.AppLauncherHelper
import com.gntr.professionalsleeper.presentation.schedule.CalendarEventUiMapper
import com.gntr.professionalsleeper.presentation.schedule.ScheduleListItem
import com.gntr.professionalsleeper.presentation.schedule.SleepSessionUiMapper
import com.gntr.professionalsleeper.presentation.schedule.SleepSessionUiModel
import com.gntr.professionalsleeper.presentation.schedule.sectograph.SectographMapper
import com.gntr.professionalsleeper.presentation.schedule.sectograph.SectographSector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val prefsRepo: AppPreferencesRepository,
    private val calendarEventDao: CalendarEventDao,
    private val authManager: IAuthManager
) : ViewModel() {

    private var allResolveInfos = emptyList<ResolveInfo>()
    private var currentPageIndex = 0
    private val pageSize = 20
    private var isFetchingApps = false

    private val _resetEvents = Channel<Unit>(Channel.BUFFERED)
    val resetEvents = _resetEvents.receiveAsFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val targetAppPackage: StateFlow<String?> = prefsRepo.targetAppPackageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSyncing: StateFlow<Boolean> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData("ImmediateCalendarSync")
        .asFlow()
        .map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val alarmRingtoneUri: StateFlow<String> = prefsRepo.alarmRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    @RequiresApi(Build.VERSION_CODES.O)
    val todayUiSessions: StateFlow<List<SleepSessionUiModel>> =
        repository.getSessionsForScheduleDisplay()
            .map { sessions ->
                sessions.map { SleepSessionUiMapper.mapToUiModel(it, context) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStartOfTodayMilli(): Long {
        return LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    // Updated to support dynamic day offsets
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getEndOfDayOffsetMilli(daysOffset: Long): Long {
        return LocalDate.now(ZoneId.systemDefault())
            .plusDays(daysOffset + 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val sleepSectors: StateFlow<List<SectographSector>> =
        repository.getSessionsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(2))
            .map { sessions -> SectographMapper.mapSleepSessionsToSectors(sessions) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    @RequiresApi(Build.VERSION_CODES.O)
    val calendarSectors: StateFlow<List<SectographSector>> =
        calendarEventDao.getEventsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(2))
            .map { events -> SectographMapper.mapCalendarEventsToSectors(events) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    val upcomingScheduleItems: StateFlow<List<ScheduleListItem>> =
        combine(
            repository.getSessionsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(2)),
            calendarEventDao.getEventsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(2))
        ) { sessions, events ->
            val sessionItems = sessions.map { session ->
                ScheduleListItem.Session(
                    session = SleepSessionUiMapper.mapToUiModel(session, context),
                    sortKey = session.startTime
                )
            }
            val eventItems = events.map { event ->
                ScheduleListItem.CalendarEvent(
                    event = CalendarEventUiMapper.mapToUiModel(event),
                    sortKey = Instant.ofEpochMilli(event.event.startTime).atZone(ZoneId.systemDefault())
                )
            }

            val sortedItems = (sessionItems + eventItems).sortedBy { it.sortKey }
            val groupedByDate = sortedItems.groupBy { it.sortKey.toLocalDate() }

            val result = mutableListOf<ScheduleListItem>()
            groupedByDate.forEach { (date, items) ->
                val sessionCount = items.count { it is ScheduleListItem.Session }
                result.add(ScheduleListItem.DateHeader(date, sessionCount, items.first().sortKey))
                result.addAll(items)
            }
            result
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleNewSession(startTime: Long, endTime: Long, type: SessionType) {
        viewModelScope.launch {
            val session = SleepSession(
                startTime = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()),
                endTime = Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault()),
                type = type,
                status = SessionStatus.SCHEDULED
            )
            val id = repository.insertSession(session)
            alarmScheduler.scheduleAlarm(session.copy(id = id))
        }
    }

    fun saveTargetApp(packageName: String) {
        viewModelScope.launch {
            prefsRepo.saveTargetAppPackage(packageName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun triggerDebugAlarm() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val debugSession = SleepSession(
                startTime = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()),
                endTime = Instant.ofEpochMilli(now + 5000).atZone(ZoneId.systemDefault()),
                type = SessionType.NAP,
                status = SessionStatus.SCHEDULED
            )
            val id = repository.insertSession(debugSession)
            alarmScheduler.scheduleAlarm(debugSession.copy(id = id))
        }
    }

    fun initializeAppList(context: Context) {
        if (allResolveInfos.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            allResolveInfos = AppLauncherHelper.getRawLauncherActivities(context)
            loadNextAppPage(context)
        }
    }

    fun loadNextAppPage(context: Context) {
        if (isFetchingApps || currentPageIndex >= allResolveInfos.size) return
        isFetchingApps = true

        viewModelScope.launch(Dispatchers.IO) {
            val endIndex = minOf(currentPageIndex + pageSize, allResolveInfos.size)
            val chunk = allResolveInfos.subList(currentPageIndex, endIndex)
            val mappedChunk = AppLauncherHelper.mapToAppInfo(context, chunk)
            _installedApps.update { current -> current + mappedChunk }
            currentPageIndex = endIndex
            isFetchingApps = false
        }
    }

    fun resetSleepSession() {
        viewModelScope.launch {
            val deletedSessions = repository.clearAllSessions()
            deletedSessions.forEach { session -> alarmScheduler.cancelAlarm(session) }
            prefsRepo.setSetupComplete(false)
            _resetEvents.send(Unit)
        }
    }

    fun triggerCalendarSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val account = authManager.getSignedInAccount()
            val accountEmail = account?.email

            if (accountEmail != null) {
                val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                    .setInputData(workDataOf("account_email" to accountEmail))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "ImmediateCalendarSync",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            } else {
                Timber.w("Calendar sync aborted: No valid signed-in account found.")
            }
        }
    }

    fun saveAlarmRingtone(uriString: String) {
        viewModelScope.launch {
            prefsRepo.saveAlarmRingtoneUri(uriString)
        }
    }
}