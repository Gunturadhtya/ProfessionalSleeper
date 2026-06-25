package com.gntr.domain.auth

import android.content.Context

interface IAuthManager {
    suspend fun signIn(context: Context): Result<AuthAccount>
    suspend fun getSignedInAccount(): AuthAccount?
    suspend fun signOut()

    suspend fun getCalendarAccessToken(): String?
}