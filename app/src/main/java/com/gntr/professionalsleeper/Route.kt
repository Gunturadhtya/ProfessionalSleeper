package com.gntr.professionalsleeper

sealed class Route(val route: String) {
    object Auth : Route("auth")
    object Setup : Route("setup")
    object Schedule : Route("schedule")
    object Settings : Route("settings")
}