package com.gntr.ui.schedule.edit

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.ui.Route
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.service.SleepSessionManager
import com.gntr.domain.model.SessionType
import com.gntr.domain.alarm.IAlarmScheduler
import com.gntr.domain.calendar.ISyncManager
import com.gntr.domain.repository.ISleepSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class EditSessionViewModel @Inject constructor(
    private val repository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val sleepSessionManager: SleepSessionManager,
    private val syncManager: ISyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long =
        savedStateHandle.get<Long>(Route.EditSession.ARG_SESSION_ID) ?: -1L

    private val _uiState = MutableStateFlow(EditSessionUiState(sessionId = sessionId))
    val uiState: StateFlow<EditSessionUiState> = _uiState.asStateFlow()

    private val _saveEvents = Channel<Unit>(Channel.BUFFERED)
    val saveEvents = _saveEvents.receiveAsFlow()

    init {
        loadSession()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSession() {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                _uiState.value = EditSessionUiState(
                    isLoading = false,
                    sessionId = session.id,
                    type = session.type,
                    status = session.status,
                    startTimeMillis = session.startTime.toInstant().toEpochMilli(),
                    endTimeMillis = session.endTime.toInstant().toEpochMilli()
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onTypeChanged(type: SessionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun onStartTimeChanged(newStartTimeMillis: Long) {
        val current = _uiState.value
        val duration = current.endTimeMillis - current.startTimeMillis
        _uiState.value = current.copy(
            startTimeMillis = newStartTimeMillis,
            endTimeMillis = newStartTimeMillis + duration
        )
    }

    fun onEndTimeChanged(newEndTimeMillis: Long) {
        _uiState.value = _uiState.value.copy(endTimeMillis = newEndTimeMillis)
    }

    fun onDurationMinutesChanged(minutes: Long) {
        val current = _uiState.value
        val safeMinutes = minutes.coerceAtLeast(1L)
        _uiState.value = current.copy(
            endTimeMillis = current.startTimeMillis + (safeMinutes * 60_000L)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveSession() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.sessionId == -1L || state.endTimeMillis <= state.startTimeMillis) return@launch

            val existing = repository.getSessionById(state.sessionId) ?: return@launch

            val updated = existing.copy(
                type = state.type,
                startTime = Instant.ofEpochMilli(state.startTimeMillis).atZone(ZoneId.systemDefault()),
                endTime = Instant.ofEpochMilli(state.endTimeMillis).atZone(ZoneId.systemDefault())
            )

            sleepSessionManager.updateScheduled(updated)
            syncManager.triggerCalendarSync()
            _saveEvents.send(Unit)
        }
    }
}