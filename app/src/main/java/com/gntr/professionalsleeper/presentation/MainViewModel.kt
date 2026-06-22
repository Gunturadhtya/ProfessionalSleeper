package com.gntr.professionalsleeper.presentation

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
}