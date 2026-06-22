package com.gntr.professionalsleeper.presentation.auth

import android.content.Context

sealed interface AuthEvent {
    data class OnEmailChanged(val email: String) : AuthEvent
    data class OnPasswordChanged(val password: String) : AuthEvent
    object TogglePasswordVisibility : AuthEvent
    object ToggleAuthMode : AuthEvent
    object ClearError : AuthEvent
    object SubmitEmailPassword : AuthEvent
    data class SubmitGoogleSignIn(val context: Context) : AuthEvent
    object ClearAuthorizationIntent : AuthEvent
    data class OnAuthorizationResolved(val context: Context, val isGranted: Boolean) : AuthEvent
}