package com.gntr.domain.auth

interface ISecureTokenStore {
    fun saveTokens(id: String, email: String, idToken: String, displayName: String? = null, photoUrl: String? = null)
    fun getUserId(): String?
    fun getUserEmail(): String?
    fun getIdToken(): String?
    fun getDisplayName(): String?
    fun getPhotoUrl(): String?
    fun clearTokens()
}