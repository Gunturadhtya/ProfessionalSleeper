package com.gntr.professionalsleeper.presentation.profile

import com.gntr.professionalsleeper.domain.auth.AuthAccount

data class ProfileState(
    val isLoading: Boolean = true,
    val account: AuthAccount? = null
) {
    val isLoggedIn: Boolean get() = account != null
}