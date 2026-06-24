package com.gntr.professionalsleeper.domain.model

data class CalendarSource(
    val id: String,
    val displayName: String,
    val colorHex: String,
    val isEnabled: Boolean = true
)