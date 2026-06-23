package com.gntr.professionalsleeper.presentation

import CalendarSyncWorker
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
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import com.gntr.professionalsleeper.framework.launcher.AppInfo
import com.gntr.professionalsleeper.framework.launcher.AppLauncherHelper
import com.gntr.professionalsleeper.presentation.schedule.sectograph.SectographMapper
import com.gntr.professionalsleeper.presentation.schedule.sectograph.SectographSector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val prefsRepo: AppPreferencesRepository
) : ViewModel() {

    private var allResolveInfos = emptyList<ResolveInfo>()
    private var currentPageIndex = 0
    private val pageSize = 20
    private var isFetchingApps = false

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _resetEvents = Channel<Unit>(Channel.BUFFERED)
    val resetEvents = _resetEvents.receiveAsFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val targetAppPackage: StateFlow<String?> = prefsRepo.targetAppPackageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @RequiresApi(Build.VERSION_CODES.O)
    val todaySessions: StateFlow<List<SleepSession>> =
        repository.getSessionsForScheduleDisplay()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    @RequiresApi(Build.VERSION_CODES.O)
    val sleepSectors: StateFlow<List<SectographSector>> =
        repository.getSessionsForScheduleDisplay()
            .map { sessions -> SectographMapper.mapSleepSessionsToSectors(sessions) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

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
            alarmScheduler.scheduleAlarm(session.copy(id = id.toInt()))
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
            alarmScheduler.scheduleAlarm(debugSession.copy(id = id.toInt()))
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

    fun triggerCalendarSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1500)
            } finally {
                _isSyncing.value = false
            }
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

    fun getSyncState(context: Context): StateFlow<Boolean> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("ImmediateCalendarSync")
            .asFlow()
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }

    fun triggerCalendarSync(context: Context, accountEmail: String) {
        val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setInputData(workDataOf("account_email" to accountEmail))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ImmediateCalendarSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}