package com.gntr.professionalsleeper.presentation

import android.content.Context
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import com.gntr.professionalsleeper.framework.launcher.AppInfo
import com.gntr.professionalsleeper.framework.launcher.AppLauncherHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()
    val todaySessions: StateFlow<List<SleepSession>> = repository.getSessionsForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val targetAppPackage: StateFlow<String?> = prefsRepo.targetAppPackageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}