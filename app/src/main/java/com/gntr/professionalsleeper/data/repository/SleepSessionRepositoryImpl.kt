package com.gntr.professionalsleeper.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.mapper.toDomain
import com.gntr.professionalsleeper.data.local.mapper.toEntity
import com.gntr.professionalsleeper.domain.model.SleepSession
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
class SleepSessionRepositoryImpl(
    private val dao: SleepSessionDao
) : ISleepSessionRepository {

    override suspend fun insertSession(session: SleepSession): Long = dao.insertSession(session.toEntity())

    override suspend fun updateSession(session: SleepSession) = dao.updateSession(session.toEntity())


    override fun getSessionsForToday(): Flow<List<SleepSession>> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        return dao.getAllSessions().map { sessions ->
            sessions
                .filter { it.startTime.toInstant().toEpochMilli() in startOfDay..endOfDay }
                .map { it.toDomain() }
        }
    }

    override fun getSessionsForScheduleDisplay(): Flow<List<SleepSession>> {
        return dao.getAllSessions().map { all ->
            all.map { it.toDomain() }
        }
    }

    override suspend fun getSessionsSnapshotForDay(startOfDay: Long, endOfDay: Long): List<SleepSession> {
        return dao.getAllSessionsSnapshot()
            .filter { session -> session.startTime.toInstant().toEpochMilli() in startOfDay..endOfDay }
            .map { it.toDomain() }
    }

    override suspend fun getSessionById(id: Long): SleepSession? = dao.getSessionById(id)?.toDomain()

    override suspend fun clearAllSessions(): List<SleepSession> {
        val allSessions = dao.getAllSessionsSnapshot()
        dao.deleteAllSessions()
        return allSessions.map { it.toDomain() }
    }

    override fun getSessionsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): Flow<List<SleepSession>> {
        return dao.getAllSessions().map { sessions ->
            sessions.filter {
                it.startTime.toInstant().toEpochMilli() in startEpochMilli..endEpochMilli
            }.map { it.toDomain() }
        }
    }

    override suspend fun getSessionsSnapshotForTimeframe(startEpochMilli: Long, endEpochMilli: Long): List<SleepSession> {
        return dao.getAllSessionsSnapshot().filter { session ->
            session.startTime.toInstant().toEpochMilli() in startEpochMilli..endEpochMilli
        }.map { it.toDomain() }
    }
}