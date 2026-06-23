package com.gntr.professionalsleeper.presentation.ringing

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class RingingViewModel @Inject constructor(
    private val sleepSessionDao: SleepSessionDao,
    private val alarmScheduler: IAlarmScheduler
) : ViewModel() {

    private val _currentSession = MutableStateFlow<SleepSession?>(null)
    val currentSession: StateFlow<SleepSession?> = _currentSession.asStateFlow()

    fun loadSession(sessionId: Int) {
        viewModelScope.launch {
            _currentSession.value = sleepSessionDao.getSessionById(sessionId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun triggerSnooze(session: SleepSession, snoozeMinutes: Long = 5) {
        viewModelScope.launch {
            val updatedSession = session.copy(
                endTime = ZonedDateTime.now().plusMinutes(snoozeMinutes),
                snoozeCount = session.snoozeCount + 1
            )
            sleepSessionDao.updateSession(updatedSession)
            alarmScheduler.scheduleAlarm(updatedSession)
        }
    }
}