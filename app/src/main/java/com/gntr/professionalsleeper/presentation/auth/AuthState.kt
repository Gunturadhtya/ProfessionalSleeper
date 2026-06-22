package com.gntr.professionalsleeper.presentation.auth

import android.app.PendingIntent

data class AuthState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingAuthorization: PendingIntent? = null
)