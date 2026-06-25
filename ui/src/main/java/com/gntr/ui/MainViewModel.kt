package com.gntr.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.auth.IAuthManager
import com.gntr.domain.calendar.ISyncManager
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.domain.repository.ISleepSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import com.gntr.domain.usecase.ScheduleNewSessionUseCase
import com.gntr.domain.usecase.TriggerDebugAlarmUseCase
import com.gntr.ui.schedule.CalendarEventUiMapper
import com.gntr.ui.schedule.ScheduleListItem
import com.gntr.ui.schedule.SleepSessionUiMapper
import com.gntr.ui.schedule.SleepSessionUiModel
import com.gntr.ui.schedule.sectograph.SectographMapper
import com.gntr.ui.schedule.sectograph.SectographSector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ISleepSessionRepository,
    private val calendarEventRepository: ICalendarEventRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val prefsRepo: IPreferencesRepository,
    private val authManager: IAuthManager,
    private val syncManager: ISyncManager,
    private val scheduleNewSessionUseCase: ScheduleNewSessionUseCase,
    private val triggerDebugAlarmUseCase: TriggerDebugAlarmUseCase
) : ViewModel() {

    private val _resetEvents = Channel<Unit>(Channel.BUFFERED)
    val resetEvents: Flow<Unit> = _resetEvents.receiveAsFlow()

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
        repository.getSessionsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(1))
            .map { sessions -> SectographMapper.mapSleepSessionsToSectors(sessions) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    @RequiresApi(Build.VERSION_CODES.O)
    val calendarSectors: StateFlow<List<SectographSector>> =
        calendarEventRepository.getEventsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(1))
            .map { events -> SectographMapper.mapCalendarEventsToSectors(events) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    val upcomingScheduleItems: StateFlow<List<ScheduleListItem>> =
        combine(
            repository.getSessionsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(3)),
            calendarEventRepository.getEventsForTimeframe(getStartOfTodayMilli(), getEndOfDayOffsetMilli(3))
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
                    sortKey = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
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
            scheduleNewSessionUseCase(startTime, endTime, type)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun triggerDebugAlarm() {
        viewModelScope.launch {
            triggerDebugAlarmUseCase()
        }
    }

    fun resetSleepSession() {
        viewModelScope.launch {
            val deletedSessions = repository.clearAllSessions()
            deletedSessions.forEach { session -> alarmScheduler.cancelAlarm(session) }

            calendarEventRepository.deleteAll()

            prefsRepo.setSetupComplete(false)
            _resetEvents.send(Unit)
        }
    }

    fun triggerCalendarSync() {
        viewModelScope.launch {
            val account = authManager.getSignedInAccount()
            if (account?.email != null) {
                syncManager.triggerCalendarSync()
            } else {
                Timber.w("Calendar sync aborted: No valid signed-in account found.")
            }
        }
    }
}