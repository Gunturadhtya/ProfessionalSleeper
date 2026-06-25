package com.gntr.ui.auth

import android.content.Context

sealed interface AuthEvent {
    object ClearError : AuthEvent
    data class SubmitGoogleSignIn(val context: Context) : AuthEvent
    object ClearAuthorizationIntent : AuthEvent
    data class OnAuthorizationResolved(val context: Context, val isGranted: Boolean) : AuthEvent
    object ContinueOffline : AuthEvent
}