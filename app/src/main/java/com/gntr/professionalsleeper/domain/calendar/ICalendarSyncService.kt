package com.gntr.professionalsleeper.domain.calendar

import com.gntr.professionalsleeper.data.local.entity.CalendarSourceEntity

interface ICalendarSyncService {
    suspend fun fetchAvailableCalendars(accessToken: String): Result<List<CalendarSourceEntity>>
    suspend fun fetchUpcomingEvents(accessToken: String, sourceId: String, timeMin: Long, timeMax: Long): Result<List<CalendarEvent>>
    fun checkScheduleConflict(napStartTime: Long, napEndTime: Long, events: List<CalendarEvent>): Boolean
}