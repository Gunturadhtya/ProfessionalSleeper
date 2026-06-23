package com.gntr.professionalsleeper.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.professionalsleeper.data.local.dao.CalendarSourceDao
import com.gntr.professionalsleeper.data.local.entity.CalendarSourceEntity
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import com.gntr.professionalsleeper.domain.usecase.ReconcileCalendarsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: IAuthManager,
    private val calendarSourceDao: CalendarSourceDao,
    private val reconcileCalendarsUseCase: ReconcileCalendarsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    val calendarSources: StateFlow<List<CalendarSourceEntity>> = calendarSourceDao.getAllSourcesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val account = authManager.getSignedInAccount()
            _state.update { it.copy(isLoading = false, account = account) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.signOut()
            _state.update { it.copy(account = null) }
        }
    }

    fun syncCalendarList() {
        viewModelScope.launch {
            val token = authManager.getCalendarAccessToken()
            if (token != null) {
                reconcileCalendarsUseCase(token)
            }
        }
    }

    fun toggleCalendarSource(source: CalendarSourceEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            calendarSourceDao.updateSource(source.copy(isEnabled = isEnabled))
        }
    }
}