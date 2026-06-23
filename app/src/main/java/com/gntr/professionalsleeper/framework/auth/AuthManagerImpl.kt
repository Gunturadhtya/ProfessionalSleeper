package com.gntr.professionalsleeper.framework.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
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
import com.gntr.professionalsleeper.data.local.security.SecureTokenManager
import com.gntr.professionalsleeper.domain.auth.AuthAccount
import com.gntr.professionalsleeper.domain.auth.AuthorizationRequiredException
import com.gntr.professionalsleeper.domain.auth.IAuthManager
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

class AuthManagerImpl(
    private val context: Context,
    private val secureTokenManager: SecureTokenManager
) : IAuthManager {

    private val credentialManager = CredentialManager.create(context)
    private val authorizationClient = Identity.getAuthorizationClient(context)
    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
        .setAudience(listOf(BuildConfig.ANDROID_CLIENT_ID))
        .build()

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
                Timber.e("Sign-in failed: Unrecognized credential type received.")
                return@withContext Result.failure(Exception("Unrecognized credential type"))
            }

            val googleIdCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse GoogleIdTokenCredential")
                return@withContext Result.failure(e)
            }

            Timber.d("Identity verification successful for user.")

            val userEmail = extractEmailFromIdToken(googleIdCredential.idToken)
                ?: googleIdCredential.id

            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(CalendarScopes.CALENDAR_READONLY)))
                .requestOfflineAccess(BuildConfig.ANDROID_CLIENT_ID)
                .build()

            val authResult = authorizationClient.authorize(authRequest).await()

            if (authResult.hasResolution()) {
                Timber.w("OAuth2 scopes require user resolution.")
                return@withContext Result.failure(
                    AuthorizationRequiredException(authResult.pendingIntent!!)
                )
            }

            secureTokenManager.saveTokens(
                id = googleIdCredential.id,
                email = userEmail,
                idToken = googleIdCredential.idToken,
                displayName = googleIdCredential.displayName,
                photoUrl = googleIdCredential.profilePictureUri?.toString()
            )

            Result.success(
                AuthAccount(
                    id = googleIdCredential.id,
                    email = userEmail,
                    idToken = googleIdCredential.idToken,
                    serverAuthCode = authResult.serverAuthCode,
                    displayName = googleIdCredential.displayName,
                    photoUrl = googleIdCredential.profilePictureUri?.toString()
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Sign-in or Authorization process failed")
            Result.failure(e)
        }
    }

    override suspend fun getSignedInAccount(): AuthAccount? = withContext(Dispatchers.IO) {
        val idToken = secureTokenManager.getIdToken()
        val userId = secureTokenManager.getUserId()
        var userEmail = secureTokenManager.getUserEmail()

        if (idToken.isNullOrEmpty() || userId.isNullOrEmpty() || userEmail.isNullOrEmpty()) {
            return@withContext null
        }

        if (!isTokenValid(idToken)) {
            secureTokenManager.clearTokens()
            return@withContext null
        }

        if (!userEmail.contains("@")) {
            val realEmail = extractEmailFromIdToken(idToken)
            if (realEmail != null) {
                userEmail = realEmail
                secureTokenManager.saveTokens(
                    id = userId,
                    email = realEmail,
                    idToken = idToken,
                    displayName = secureTokenManager.getDisplayName(),
                    photoUrl = secureTokenManager.getPhotoUrl()
                )
            } else {
                secureTokenManager.clearTokens()
                return@withContext null
            }
        }

        AuthAccount(
            id = userId,
            email = userEmail,
            idToken = idToken,
            displayName = secureTokenManager.getDisplayName(),
            photoUrl = secureTokenManager.getPhotoUrl()
        )
    }

    /**
     * Re-runs the authorization request for the already-consented Calendar
     * scope. When the user has previously granted access, this resolves
     * silently (no PendingIntent, no UI) and returns a fresh access token —
     * this is the correct way to get a usable bearer token for Credential
     * Manager / One Tap sign-ins, since no on-device AccountManager Account
     * is ever created for them.
     */
    override suspend fun getCalendarAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(CalendarScopes.CALENDAR_READONLY)))
                .build()

            val authResult = authorizationClient.authorize(authRequest).await()

            if (authResult.hasResolution()) {
                Timber.w("Calendar authorization requires user resolution; cannot resolve silently in the background.")
                return@withContext null
            }

            authResult.accessToken
        } catch (e: Exception) {
            Timber.e(e, "Failed to silently obtain a Calendar access token.")
            null
        }
    }

    override suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        val tokenToRevoke = secureTokenManager.getIdToken()

        if (!tokenToRevoke.isNullOrEmpty()) {
            revokeGoogleToken(tokenToRevoke)
        }

        try {
            secureTokenManager.clearTokens()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Timber.e(e, "Error occurred during local credential wipe.")
        }
    }

    private fun revokeGoogleToken(token: String) {
        try {
            val url = URL("https://oauth2.googleapis.com/revoke?token=$token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Timber.w("Server rejected token revocation. HTTP Code: $responseCode")
            }
        } catch (e: Exception) {
            Timber.e(e, "Network failure preventing server-side token revocation.")
        }
    }

    private fun isTokenExpired(jwt: String): Boolean {
        return try {
            val split = jwt.split(".")
            if (split.size < 2) return true

            val payloadBytes = Base64.decode(split[1], Base64.URL_SAFE)
            val payloadString = String(payloadBytes, Charsets.UTF_8)
            val jsonObject = JSONObject(payloadString)

            val exp = jsonObject.getLong("exp")
            val currentTimeSeconds = System.currentTimeMillis() / 1000

            currentTimeSeconds >= (exp - 300)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode JWT to check expiration")
            true
        }
    }

    private fun isTokenValid(idTokenString: String): Boolean {
        return try {
            val validToken = verifier.verify(idTokenString)
            validToken != null
        } catch (e: Exception) {
            Timber.e(e, "Cryptographic JWT validation failed.")
            false
        }
    }

    private fun extractEmailFromIdToken(idToken: String): String? {
        return try {
            val split = idToken.split(".")
            if (split.size < 2) return null

            val payloadBytes = Base64.decode(split[1], Base64.URL_SAFE)
            val payloadString = String(payloadBytes, Charsets.UTF_8)
            val jsonObject = JSONObject(payloadString)

            jsonObject.optString("email").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode JWT to extract email")
            null
        }
    }
}