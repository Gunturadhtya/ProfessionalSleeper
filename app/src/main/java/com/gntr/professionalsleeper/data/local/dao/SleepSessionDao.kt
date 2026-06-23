package com.gntr.professionalsleeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSession): Long

    @Update
    suspend fun updateSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :startOfDay AND startTime <= :endOfDay ORDER BY startTime ASC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :startOfDay AND startTime <= :endOfDay")
    suspend fun getSessionsSnapshotForDay(startOfDay: Long, endOfDay: Long): List<SleepSession>
}