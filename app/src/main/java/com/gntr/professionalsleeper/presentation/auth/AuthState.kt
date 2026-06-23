package com.gntr.professionalsleeper.presentation.auth

import android.app.PendingIntent

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingAuthorization: PendingIntent? = null
)