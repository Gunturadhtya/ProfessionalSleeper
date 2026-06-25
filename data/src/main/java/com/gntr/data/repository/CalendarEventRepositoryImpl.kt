package com.gntr.data.repository

import com.gntr.data.local.dao.CalendarEventDao
import com.gntr.domain.repository.ICalendarEventRepository
import com.gntr.data.local.entity.CalendarEventWithSourceEntity
import com.gntr.data.local.mapper.toEntity
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.CalendarEventDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.mapNotNull

class CalendarEventRepositoryImpl @Inject constructor(
    private val calendarEventDao: CalendarEventDao
) : ICalendarEventRepository {

    override fun getEventsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): Flow<List<CalendarEventDetail>> {
        return calendarEventDao.getEventsForTimeframe(startEpochMilli, endEpochMilli)
            .map { relations ->
                relations.mapNotNull { it.toDomainDetailOrNull() }
            }
            .flowOn(Dispatchers.Default)
    }

    override suspend fun deleteAll() {
        calendarEventDao.deleteAll()
    }

    override suspend fun getEventIdsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): List<String> {
        return calendarEventDao.getEventIdsForTimeframe(startEpochMilli, endEpochMilli)
    }

    override suspend fun deleteEventsByIds(eventIds: List<String>) {
        calendarEventDao.deleteEventsByIds(eventIds)
    }

    override suspend fun insertAll(events: List<CalendarEvent>) {
        val entities = events.map { it.toEntity() }
        calendarEventDao.insertAll(entities)
    }
}

private fun CalendarEventWithSourceEntity.toDomainDetailOrNull(): CalendarEventDetail? {
    if (this.source == null) {
        Timber.e("Data Integrity Breach: CalendarEvent [${this.event.id}] is orphaned. Missing parent CalendarSource [${this.event.sourceId}]. Dropping from Domain stream.")
        return null
    }

    return CalendarEventDetail(
        id = this.event.id,
        title = this.event.title,
        startTime = this.event.startTime,
        endTime = this.event.endTime,
        sourceName = this.source.displayName,
        sourceColorHex = this.source.colorHex
    )
}