package com.gntr.ui.schedule

data class CalendarEventUiModel(
    val id: String,
    val title: String,
    val timeRange: String,
    val tagLabel: String,
    val tagColorHex: String
)