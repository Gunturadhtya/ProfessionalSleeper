package com.gntr.professionalsleeper.presentation.schedule

data class CalendarEventUiModel(
    val id: String,
    val title: String,
    val timeRange: String,
    val tagLabel: String,
    val tagColorHex: String
)