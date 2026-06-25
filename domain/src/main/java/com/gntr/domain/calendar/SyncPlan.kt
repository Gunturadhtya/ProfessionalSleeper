package com.gntr.domain.calendar

import com.gntr.domain.model.CalendarEvent

data class SyncPlan(
    val eventIdsToDelete: List<String>,
    val eventsToInsert: List<CalendarEvent>,
    val sourceIdsToDisable: List<String>,
    val requiresRetry: Boolean
)