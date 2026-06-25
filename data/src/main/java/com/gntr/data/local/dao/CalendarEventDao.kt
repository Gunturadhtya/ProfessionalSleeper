package com.gntr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gntr.data.local.entity.CalendarEventEntity
import com.gntr.data.local.entity.CalendarEventWithSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE endTime < :currentTime")
    suspend fun deleteExpiredEvents(currentTime: Long)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()

    @Query("SELECT id FROM calendar_events WHERE endTime >= :timeMin AND startTime <= :timeMax")
    suspend fun getEventIdsForTimeframe(timeMin: Long, timeMax: Long): List<String>

    @Query("SELECT id FROM calendar_events WHERE sourceId = :sourceId AND endTime >= :timeMin AND startTime <= :timeMax")
    suspend fun getEventIdsForSourceAndTimeframe(sourceId: String, timeMin: Long, timeMax: Long): List<String>

    @Query("SELECT * FROM calendar_events WHERE sourceId IN (:sourceIds) AND endTime >= :timeMin AND startTime <= :timeMax")
    suspend fun getEventsForSourcesSnapshot(sourceIds: List<String>, timeMin: Long, timeMax: Long): List<CalendarEventEntity>

    @Query("DELETE FROM calendar_events WHERE id IN (:eventIds)")
    suspend fun deleteEventsByIds(eventIds: List<String>)

    @Transaction
    @Query("SELECT * FROM calendar_events WHERE endTime >= :startOfDay AND startTime <= :endOfDay ORDER BY startTime ASC")
    fun getEventsForTimeframe(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEventWithSourceEntity>>
}