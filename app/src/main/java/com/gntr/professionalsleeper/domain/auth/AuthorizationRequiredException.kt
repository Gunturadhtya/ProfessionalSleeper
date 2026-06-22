package com.gntr.professionalsleeper.domain.auth

import android.app.PendingIntent

class AuthorizationRequiredException(
    val pendingIntent: PendingIntent,
    message: String = "OAuth 2.0 Authorization Required"
) : Exception(message)