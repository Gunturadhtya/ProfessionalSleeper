package com.gntr.professionalsleeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gntr.professionalsleeper.data.local.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE endTime < :currentTime")
    suspend fun deleteExpiredEvents(currentTime: Long)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()

    @Query("SELECT * FROM calendar_events WHERE endTime >= :startOfDay AND startTime <= :endOfDay ORDER BY startTime ASC")
    fun getEventsForTimeframe(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEventEntity>>
}