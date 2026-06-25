package com.gntr.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.data.local.dao.SleepSessionDao
import com.gntr.data.local.mapper.toDomain
import com.gntr.data.local.mapper.toEntity
import com.gntr.domain.model.SleepSession
import com.gntr.domain.repository.ISleepSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class SleepSessionRepositoryImpl @Inject constructor(
    private val dao: SleepSessionDao
) : ISleepSessionRepository {

    private val writeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC)

    private fun Long.toUtcIso(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneOffset.UTC)
            .format(writeFormatter)

    override suspend fun insertSession(session: SleepSession): Long =
        dao.insertSession(session.toEntity())

    override suspend fun updateSession(session: SleepSession) =
        dao.updateSession(session.toEntity())

    override fun getSessionsForToday(): Flow<List<SleepSession>> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfYesterday = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfToday = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return dao.getSessionsForTimeframe(startOfYesterday.toUtcIso(), endOfToday.toUtcIso())
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getSessionsForScheduleDisplay(): Flow<List<SleepSession>> =
        dao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override suspend fun getSessionsSnapshotForDay(
        startOfDay: Long,
        endOfDay: Long
    ): List<SleepSession> =
        dao.getSessionsSnapshotForTimeframe(startOfDay.toUtcIso(), endOfDay.toUtcIso())
            .map { it.toDomain() }

    override suspend fun getSessionById(id: Long): SleepSession? =
        dao.getSessionById(id)?.toDomain()

    override suspend fun clearAllSessions(): List<SleepSession> {
        val all = dao.getAllSessionsSnapshot()
        dao.deleteAllSessions()
        return all.map { it.toDomain() }
    }

    override fun getSessionsForTimeframe(
        startEpochMilli: Long,
        endEpochMilli: Long
    ): Flow<List<SleepSession>> =
        dao.getSessionsForTimeframe(startEpochMilli.toUtcIso(), endEpochMilli.toUtcIso())
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getSessionsSnapshotForTimeframe(
        startEpochMilli: Long,
        endEpochMilli: Long
    ): List<SleepSession> =
        dao.getSessionsSnapshotForTimeframe(startEpochMilli.toUtcIso(), endEpochMilli.toUtcIso())
            .map { it.toDomain() }
}