package com.gntr.professionalsleeper.domain.repository

import com.gntr.professionalsleeper.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

interface ISleepSessionRepository {
    suspend fun insertSession(session: SleepSessionEntity): Long
    suspend fun updateSession(session: SleepSessionEntity)
    fun getSessionsForToday(): Flow<List<SleepSessionEntity>>
    fun getSessionsForScheduleDisplay(): Flow<List<SleepSessionEntity>>
    suspend fun getSessionsSnapshotForDay(startOfDay: Long, endOfDay: Long): List<SleepSessionEntity>
    suspend fun getSessionById(id: Long): SleepSessionEntity?
    suspend fun clearAllSessions(): List<SleepSessionEntity>
    fun getSessionsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): Flow<List<SleepSessionEntity>>
    suspend fun getSessionsSnapshotForTimeframe(startEpochMilli: Long, endEpochMilli: Long): List<SleepSessionEntity>
}