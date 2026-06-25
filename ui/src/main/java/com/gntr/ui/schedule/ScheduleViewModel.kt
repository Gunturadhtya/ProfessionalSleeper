package com.gntr.ui.schedule

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.auth.IAuthManager
import com.gntr.domain.calendar.ISyncManager
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.domain.repository.ISleepSessionRepository
import com.gntr.domain.service.SleepSessionManager
import com.gntr.domain.usecase.GetScheduleTimeframeUseCase
import com.gntr.domain.usecase.ITriggerDebugAlarmUseCase
import com.gntr.domain.usecase.ReconcileSessionStatusUseCase
import com.gntr.ui.schedule.sectograph.SectographMapper
import com.gntr.ui.schedule.sectograph.SectographSector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sleepSessionRepository: ISleepSessionRepository,
    private val sleepSessionManager: SleepSessionManager,
    private val calendarEventRepository: ICalendarEventRepository,
    private val prefsRepo: IPreferencesRepository,
    private val authManager: IAuthManager,
    private val syncManager: ISyncManager,
    private val getTimeframe: GetScheduleTimeframeUseCase,
    private val triggerDebugAlarmUseCase: ITriggerDebugAlarmUseCase,
    private val reconcileSessionStatusUseCase: ReconcileSessionStatusUseCase
) : ViewModel() {

    init {
        viewModelScope.launch {
            reconcileSessionStatusUseCase()
        }
    }

    private val _resetEvents = Channel<Unit>(Channel.BUFFERED)
    val resetEvents: Flow<Unit> = _resetEvents.receiveAsFlow()

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Compute timeframes once; getTimeframe derives from LocalDate.now() so
    // calling it six times in property initialisers returns six identical but
    // logically separate values. Storing them here also makes the intent clear
    // and prevents subtle inconsistencies if GetScheduleTimeframeUseCase ever
    // reads from a preference (e.g. "week starts on Monday").
    private val todayTimeframe = getTimeframe(startDaysOffset = 0, endDaysOffset = 0)
    private val upcomingTimeframe = getTimeframe(startDaysOffset = 0, endDaysOffset = 3)

    val sleepSectors: StateFlow<List<SectographSector>> =
        sleepSessionRepository.getSessionsForTimeframe(
            startEpochMilli = todayTimeframe.startMilli,
            endEpochMilli = todayTimeframe.endMilli
        )
            .map { sessions -> SectographMapper.mapSleepSessionsToSectors(sessions) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarSectors: StateFlow<List<SectographSector>> =
        calendarEventRepository.getEventsForTimeframe(
            startEpochMilli = todayTimeframe.startMilli,
            endEpochMilli = todayTimeframe.endMilli
        )
            .map { events -> SectographMapper.mapCalendarEventsToSectors(events) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingScheduleItems: StateFlow<List<ScheduleListItem>> =
        combine(
            sleepSessionRepository.getSessionsForTimeframe(
                startEpochMilli = upcomingTimeframe.startMilli,
                endEpochMilli = upcomingTimeframe.endMilli
            ),
            calendarEventRepository.getEventsForTimeframe(
                startEpochMilli = upcomingTimeframe.startMilli,
                endEpochMilli = upcomingTimeframe.endMilli
            )
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

    fun triggerDebugAlarm() {
        viewModelScope.launch {
            triggerDebugAlarmUseCase()
        }
    }

    fun resetSleepSession() {
        viewModelScope.launch {
            sleepSessionManager.clearAllAndCancelSideEffects()
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