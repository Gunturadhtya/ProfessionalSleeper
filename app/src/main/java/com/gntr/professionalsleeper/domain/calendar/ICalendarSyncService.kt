package com.gntr.professionalsleeper.domain.calendar

interface ICalendarSyncService {
    suspend fun fetchUpcomingEvents(accessToken: String, timeMin: Long, timeMax: Long): Result<List<CalendarEvent>>
    fun checkScheduleConflict(napStartTime: Long, napEndTime: Long, events: List<CalendarEvent>): Boolean
}