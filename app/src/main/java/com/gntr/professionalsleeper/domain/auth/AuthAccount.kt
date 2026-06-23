package com.gntr.professionalsleeper.domain.auth

data class AuthAccount(
    val id: String,
    val email: String,
    val idToken: String,
    val serverAuthCode: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)