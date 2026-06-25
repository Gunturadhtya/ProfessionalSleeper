package com.gntr.ui.quicknap

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.alarm.IAlarmController
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import com.gntr.domain.service.SleepSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class QuickNapViewModel @Inject constructor(
    private val sleepSessionManager: SleepSessionManager,
    private val alarmController: IAlarmController
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickNapUiState())
    val uiState: StateFlow<QuickNapUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var savedNapSession: SleepSession? = null

    fun onHoursChanged(hours: Int) {
        if (_uiState.value.napState != QuickNapState.IDLE) return
        _uiState.update { it.copy(selectedHours = hours.coerceIn(0, 23)) }
    }

    fun onMinutesChanged(minutes: Int) {
        if (_uiState.value.napState != QuickNapState.IDLE) return
        _uiState.update { it.copy(selectedMinutes = minutes.coerceIn(0, 59)) }
    }

    fun onSecondsChanged(seconds: Int) {
        if (_uiState.value.napState != QuickNapState.IDLE) return
        _uiState.update { it.copy(selectedSeconds = seconds.coerceIn(0, 59)) }
    }

    fun startNap() {
        val state = _uiState.value
        val total = (state.selectedHours * 3600L) + (state.selectedMinutes * 60L) + state.selectedSeconds
        if (total <= 0L) return

        val now = ZonedDateTime.now()
        val endTime = now.plusSeconds(total)

        val session = SleepSession(
            startTime = now,
            endTime = endTime,
            type = SessionType.NAP,
            status = SessionStatus.SCHEDULED
        )

        viewModelScope.launch {
            val id = sleepSessionManager.scheduleNew(session)
            savedNapSession = session.copy(id = id)
        }

        _uiState.update {
            it.copy(
                napState = QuickNapState.RUNNING,
                totalSeconds = total,
                remainingSeconds = total
            )
        }

        startCountdown(total)
    }

    private fun startCountdown(totalSeconds: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _uiState.update { it.copy(remainingSeconds = remaining) }
            }
            // Once countdown reaches 0, the OS AlarmManager will trigger AlarmReceiver
            // -> AlarmService -> RingingActivity. We just let it idle at 00:00 until then.
        }
    }

    fun cancelNap() {
        countdownJob?.cancel()

        savedNapSession?.let { session ->
            viewModelScope.launch {
                sleepSessionManager.cancelSession(session)
            }
        }

        savedNapSession = null
        _uiState.update {
            QuickNapUiState(
                selectedHours = it.selectedHours,
                selectedMinutes = it.selectedMinutes,
                selectedSeconds = it.selectedSeconds
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}