package com.gntr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gntr.data.local.entity.CalendarSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarSourceDao {
    @Query("SELECT * FROM calendar_sources ORDER BY displayName ASC")
    fun getAllSourcesFlow(): Flow<List<CalendarSourceEntity>>

    @Query("SELECT * FROM calendar_sources WHERE isEnabled = 1")
    suspend fun getEnabledSources(): List<CalendarSourceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSources(sources: List<CalendarSourceEntity>)

    @Update
    suspend fun updateSource(source: CalendarSourceEntity)

    @Query("DELETE FROM calendar_sources WHERE id NOT IN (:validIds)")
    suspend fun deleteOrphanedSources(validIds: List<String>)
}