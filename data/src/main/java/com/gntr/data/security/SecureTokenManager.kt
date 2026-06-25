package com.gntr.data.security

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gntr.domain.auth.ISecureTokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SecureTokenManager @Inject constructor(
    @ApplicationContext context: Context
) : ISecureTokenStore {

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

    override fun saveTokens(
        id: String,
        email: String,
        idToken: String,
        displayName: String?,
        photoUrl: String?
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

    override fun getUserId(): String? = sharedPreferences.getString("user_id", null)

    override fun getUserEmail(): String? = sharedPreferences.getString("user_email", null)

    override fun getIdToken(): String? = sharedPreferences.getString("id_token", null)

    override fun getDisplayName(): String? = sharedPreferences.getString("display_name", null)

    override fun getPhotoUrl(): String? = sharedPreferences.getString("photo_url", null)

    override fun clearTokens() {
        sharedPreferences.edit { clear() }
    }
}