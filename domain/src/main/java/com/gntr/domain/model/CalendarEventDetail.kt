package com.gntr.domain.model

data class CalendarEventDetail(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val sourceName: String,
    val sourceColorHex: String
)