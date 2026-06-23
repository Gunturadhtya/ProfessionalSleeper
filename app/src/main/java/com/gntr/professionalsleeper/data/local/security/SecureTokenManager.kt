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

    fun saveTokens(id: String, email: String, idToken: String, serverAuthCode: String?) {
        sharedPreferences.edit().apply {
            putString("user_id", id)
            putString("user_email", email)
            putString("id_token", idToken)
            serverAuthCode?.let { putString("server_auth_code", it) }
            apply()
        }
    }

    fun getUserId(): String? = sharedPreferences.getString("user_id", null)

    fun getUserEmail(): String? = sharedPreferences.getString("user_email", null)

    fun getIdToken(): String? = sharedPreferences.getString("id_token", null)

    fun getServerAuthCode(): String? = sharedPreferences.getString("server_auth_code", null)

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
}