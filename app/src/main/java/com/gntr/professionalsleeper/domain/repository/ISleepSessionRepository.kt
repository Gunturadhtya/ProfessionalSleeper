package com.gntr.professionalsleeper.domain.repository

import com.gntr.professionalsleeper.data.local.entity.SleepSession
import kotlinx.coroutines.flow.Flow

interface ISleepSessionRepository {
    suspend fun insertSession(session: SleepSession): Long
    suspend fun updateSession(session: SleepSession)
    fun getSessionsForToday(): Flow<List<SleepSession>>
    suspend fun getSessionsSnapshotForDay(startOfDay: Long, endOfDay: Long): List<SleepSession>
}