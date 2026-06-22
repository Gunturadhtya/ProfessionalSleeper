package com.gntr.professionalsleeper.presentation.auth

import android.content.Context
import android.util.Patterns
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

    private val _authSuccess = MutableSharedFlow<Unit>()
    val authSuccess: SharedFlow<Unit> = _authSuccess.asSharedFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnEmailChanged -> _state.update { it.copy(emailInput = event.email, errorMessage = null) }
            is AuthEvent.OnPasswordChanged -> _state.update { it.copy(passwordInput = event.password, errorMessage = null) }
            AuthEvent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            AuthEvent.ToggleAuthMode -> _state.update { it.copy(isLoginMode = !it.isLoginMode, errorMessage = null) }
            AuthEvent.ClearError -> _state.update { it.copy(errorMessage = null) }
            AuthEvent.ClearAuthorizationIntent -> _state.update { it.copy(pendingAuthorization = null) }
            AuthEvent.SubmitEmailPassword -> handleEmailPasswordSubmit()
            is AuthEvent.SubmitGoogleSignIn -> handleGoogleSignIn(event.context)
            is AuthEvent.OnAuthorizationResolved -> {
                if (event.isGranted) {
                    handleGoogleSignIn(event.context)
                } else {
                    _state.update { it.copy(errorMessage = "Calendar permissions are required.") }
                }
            }
        }
    }

    private fun handleEmailPasswordSubmit() {
        val currentState = _state.value
        if (!Patterns.EMAIL_ADDRESS.matcher(currentState.emailInput).matches()) {
            _state.update { it.copy(errorMessage = "Invalid email format") }
            return
        }
        if (currentState.passwordInput.length < 6) {
            _state.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _state.update { it.copy(isLoading = false) }
            _authSuccess.emit(Unit)
        }
    }

    private fun handleGoogleSignIn(context: Context) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authManager.signIn(context)
            _state.update { it.copy(isLoading = false) }

            result.onSuccess {
                _authSuccess.emit(Unit)
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