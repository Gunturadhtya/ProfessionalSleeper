package com.gntr.domain.repository

import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.CalendarEventDetail
import kotlinx.coroutines.flow.Flow

interface ICalendarEventRepository {
    fun getEventsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): Flow<List<CalendarEventDetail>>
    suspend fun deleteAll()
    suspend fun getEventIdsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): List<String>
    suspend fun getEventIdsForSourceAndTimeframe(sourceId: String, startEpochMilli: Long, endEpochMilli: Long): List<String>
    suspend fun getEventsForSourcesSnapshot(sourceIds: List<String>, startEpochMilli: Long, endEpochMilli: Long): List<CalendarEvent>
    suspend fun deleteEventsByIds(eventIds: List<String>)
    suspend fun insertAll(events: List<CalendarEvent>)
}