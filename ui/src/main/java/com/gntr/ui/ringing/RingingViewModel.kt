package com.gntr.ui.ringing

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.alarm.IAlarmController
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.alarm.IAppLauncher
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.domain.repository.ISleepSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class RingingViewModel @Inject constructor(
    private val sleepSessionRepository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val alarmController: IAlarmController,
    private val appLauncher: IAppLauncher,
    private val prefsRepo: IPreferencesRepository
) : ViewModel() {
    private val _currentSession = MutableStateFlow<SleepSession?>(null)
    val currentSession: StateFlow<SleepSession?> = _currentSession.asStateFlow()

    fun startAlarmService() {
        alarmController.startRinging()
    }

    fun dismissAlarmAndWork() {
        alarmController.stopRinging()
        viewModelScope.launch {
            val targetPackage = prefsRepo.targetAppPackageFlow.first()
            appLauncher.launchTargetApp(targetPackage)
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _currentSession.value = sleepSessionRepository.getSessionById(sessionId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun triggerSnooze(session: SleepSession, snoozeMinutes: Long = 5) {
        alarmController.stopRinging()
        viewModelScope.launch {
            val updatedSession = session.copy(
                endTime = ZonedDateTime.now().plusMinutes(snoozeMinutes),
                snoozeCount = session.snoozeCount + 1
            )
            sleepSessionRepository.updateSession(updatedSession)
            alarmScheduler.scheduleAlarm(updatedSession)
        }
    }
}