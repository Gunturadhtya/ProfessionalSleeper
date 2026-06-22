package com.gntr.professionalsleeper.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
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

    fun scheduleNewSession(startTime: Long, endTime: Long, type: SessionType) {
        viewModelScope.launch {
            val session = SleepSession(
                startTime = startTime,
                endTime = endTime,
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

    fun triggerDebugAlarm() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val debugSession = SleepSession(
                startTime = now,
                endTime = now + 5000,
                type = SessionType.NAP,
                status = SessionStatus.SCHEDULED
            )
            val id = repository.insertSession(debugSession)
            alarmScheduler.scheduleAlarm(debugSession.copy(id = id.toInt()))
        }
    }
}