package com.gntr.professionalsleeper.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.professionalsleeper.domain.auth.AuthorizationRequiredException
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: IAuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _authSuccess = MutableSharedFlow<String?>()
    val authSuccess: SharedFlow<String?> = _authSuccess.asSharedFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            AuthEvent.ClearError -> _state.update { it.copy(errorMessage = null) }
            AuthEvent.ClearAuthorizationIntent -> _state.update { it.copy(pendingAuthorization = null) }
            is AuthEvent.SubmitGoogleSignIn -> handleGoogleSignIn(event.context)
            is AuthEvent.OnAuthorizationResolved -> {
                if (event.isGranted) {
                    handleGoogleSignIn(event.context)
                } else {
                    _state.update { it.copy(errorMessage = "Calendar permissions are required to sync naps.") }
                }
            }
            AuthEvent.ContinueOffline -> {
                viewModelScope.launch {
                    _authSuccess.emit(null)
                }
            }
        }
    }

    private fun handleGoogleSignIn(context: Context) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authManager.signIn(context)
            _state.update { it.copy(isLoading = false) }

            result.onSuccess { account ->
                _authSuccess.emit(account.email)
            }.onFailure { exception ->
                if (exception is AuthorizationRequiredException) {
                    _state.update { it.copy(pendingAuthorization = exception.pendingIntent) }
                } else {
                    _state.update { it.copy(errorMessage = exception.message ?: "Google Sign-In failed") }
                }
            }
        }
    }
}