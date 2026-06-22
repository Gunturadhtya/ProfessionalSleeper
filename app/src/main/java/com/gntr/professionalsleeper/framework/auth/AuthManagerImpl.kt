package com.gntr.professionalsleeper.framework.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.services.calendar.CalendarScopes
import com.gntr.professionalsleeper.BuildConfig
import com.gntr.professionalsleeper.domain.auth.AuthAccount
import com.gntr.professionalsleeper.domain.auth.AuthorizationRequiredException
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class AuthManagerImpl(
    private val context: Context
) : IAuthManager {

    private val credentialManager = CredentialManager.create(context)
    private val authorizationClient = Identity.getAuthorizationClient(context)

    override suspend fun signIn(context: Context): Result<AuthAccount> = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.ANDROID_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Timber.e("Sign-in failed: Unrecognized credential type received. Type: ${credential::class.java.simpleName}")
                return@withContext Result.failure(Exception("Unrecognized credential type"))
            }

            val googleIdCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse GoogleIdTokenCredential")
                return@withContext Result.failure(e)
            }

            Timber.d("Identity verification successful for user: ${googleIdCredential.id}")

            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(CalendarScopes.CALENDAR_READONLY)))
                .requestOfflineAccess(BuildConfig.ANDROID_CLIENT_ID)
                .build()

            val authResult = authorizationClient.authorize(authRequest).await()

            if (authResult.hasResolution()) {
                Timber.w("OAuth2 scopes require user resolution (PendingIntent generated).")
                return@withContext Result.failure(
                    AuthorizationRequiredException(authResult.pendingIntent!!)
                )
            }

            Timber.i("OAuth2 Authorization successful. Server Auth Code retrieved.")

            Result.success(
                AuthAccount(
                    id = googleIdCredential.id,
                    email = googleIdCredential.id,
                    idToken = googleIdCredential.idToken,
                    serverAuthCode = authResult.serverAuthCode
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Sign-in or Authorization process failed")
            Result.failure(e)
        }
    }

    override suspend fun getSignedInAccount(): AuthAccount? = null

    override suspend fun signOut() {}
}