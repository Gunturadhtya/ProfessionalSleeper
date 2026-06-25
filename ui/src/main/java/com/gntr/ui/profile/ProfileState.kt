package com.gntr.ui.profile

import com.gntr.domain.auth.AuthAccount

data class ProfileState(
    val isLoading: Boolean = true,
    val account: AuthAccount? = null
) {
    val isLoggedIn: Boolean get() = account != null
}