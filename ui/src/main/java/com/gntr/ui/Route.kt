package com.gntr.ui

sealed class Route(val route: String) {
    object Auth : Route("auth")
    object Setup : Route("setup")
    object Schedule : Route("schedule")
    object Settings : Route("settings")
    object Profile : Route("profile")
    object QuickNap : Route("quick_nap")
    object EditSession : Route("edit_session/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"
        fun createRoute(sessionId: Long) = "edit_session/$sessionId"
    }
}