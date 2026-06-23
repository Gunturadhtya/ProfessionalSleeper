package com.gntr.professionalsleeper.domain.repository

import com.gntr.professionalsleeper.data.local.entity.SleepSession
import kotlinx.coroutines.flow.Flow

interface ISleepSessionRepository {
    suspend fun insertSession(session: SleepSession): Long
    suspend fun updateSession(session: SleepSession)
    fun getSessionsForToday(): Flow<List<SleepSession>>
    fun getSessionsForScheduleDisplay(): Flow<List<SleepSession>>
    suspend fun getSessionsSnapshotForDay(startOfDay: Long, endOfDay: Long): List<SleepSession>
    suspend fun getSessionById(id: Int): SleepSession?
    suspend fun clearAllSessions(): List<SleepSession>
    fun getSessionsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): Flow<List<SleepSession>>
    suspend fun getSessionsSnapshotForTimeframe(startEpochMilli: Long, endEpochMilli: Long): List<SleepSession>
}