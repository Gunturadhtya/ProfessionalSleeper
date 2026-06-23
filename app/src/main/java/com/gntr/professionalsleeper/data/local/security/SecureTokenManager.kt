package com.gntr.professionalsleeper.data.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(
        id: String,
        email: String,
        idToken: String,
        displayName: String? = null,
        photoUrl: String? = null
    ) {
        sharedPreferences.edit().apply {
            putString("user_id", id)
            putString("user_email", email)
            putString("id_token", idToken)
            displayName?.let { putString("display_name", it) }
            photoUrl?.let { putString("photo_url", it) }
            remove("server_auth_code")
            apply()
        }
    }

    fun getUserId(): String? = sharedPreferences.getString("user_id", null)

    fun getUserEmail(): String? = sharedPreferences.getString("user_email", null)

    fun getIdToken(): String? = sharedPreferences.getString("id_token", null)

    fun getDisplayName(): String? = sharedPreferences.getString("display_name", null)

    fun getPhotoUrl(): String? = sharedPreferences.getString("photo_url", null)

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
}