package com.gntr.domain.calendar

import com.gntr.domain.model.CalendarSource
import com.gntr.domain.model.CalendarEvent

interface ICalendarSyncService {
    suspend fun fetchAvailableCalendars(accessToken: String): Result<List<CalendarSource>>
    suspend fun fetchUpcomingEvents(accessToken: String, sourceId: String, timeMin: Long, timeMax: Long): Result<List<CalendarEvent>>
}