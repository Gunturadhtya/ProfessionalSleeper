package com.gntr.professionalsleeper.domain.calendar

data class CalendarEvent(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long
)