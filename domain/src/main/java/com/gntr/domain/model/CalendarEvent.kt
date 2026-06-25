package com.gntr.domain.model

data class CalendarEvent(
    val id: String,
    val sourceId: String,
    val title: String,
    val startTime: Long,
    val endTime: Long
)