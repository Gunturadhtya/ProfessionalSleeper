package com.gntr.professionalsleeper.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.domain.model.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSessionEntity
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepSessionRepositoryImpl(
    private val dao: SleepSessionDao
) : ISleepSessionRepository {

    override suspend fun insertSession(session: SleepSessionEntity): Long = dao.insertSession(session)

    override suspend fun updateSession(session: SleepSessionEntity) = dao.updateSession(session)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getSessionsForToday(): Flow<List<SleepSessionEntity>> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        return dao.getAllSessions().map { sessions ->
            sessions.filter { it.startTime.toInstant().toEpochMilli() in startOfDay..endOfDay }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getSessionsForScheduleDisplay(): Flow<List<SleepSessionEntity>> {
        val zoneId = ZoneId.systemDefault()

        return dao.getAllSessions().map { all ->
            val now = ZonedDateTime.now(zoneId)

            val today = LocalDate.now(zoneId)
            val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfToday = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            val endOfTomorrow = today.plusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

            val todayNaps = all.filter { session ->
                session.type == SessionType.NAP &&
                        session.startTime.toInstant().toEpochMilli() in startOfToday..endOfToday
            }

            val coreSession: SleepSessionEntity? = run {
                val todayFutureCore = all.firstOrNull { session ->
                    session.type == SessionType.CORE &&
                            session.endTime.toInstant().toEpochMilli() in startOfToday..endOfToday &&
                            session.endTime.isAfter(now)
                }
                if (todayFutureCore != null) return@run todayFutureCore

                val nextFutureCore = all
                    .filter { session ->
                        session.type == SessionType.CORE &&
                                session.endTime.toInstant().toEpochMilli() in
                                (endOfToday + 1)..endOfTomorrow
                    }
                    .minByOrNull { it.startTime.toInstant().toEpochMilli() }
                if (nextFutureCore != null) return@run nextFutureCore

                all.filter { session ->
                    session.type == SessionType.CORE &&
                            session.endTime.toInstant().toEpochMilli() in startOfToday..endOfToday
                }.maxByOrNull { it.endTime.toInstant().toEpochMilli() }
            }

            val result = (todayNaps + listOfNotNull(coreSession))
                .sortedBy { it.startTime.toInstant().toEpochMilli() }

            result
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getSessionsSnapshotForDay(startOfDay: Long, endOfDay: Long): List<SleepSessionEntity> {
        return dao.getAllSessionsSnapshot().filter { session ->
            session.startTime.toInstant().toEpochMilli() in startOfDay..endOfDay
        }
    }

    override suspend fun getSessionById(id: Long): SleepSessionEntity? = dao.getSessionById(id)

    override suspend fun clearAllSessions(): List<SleepSessionEntity> {
        val allSessions = dao.getAllSessionsSnapshot()
        dao.deleteAllSessions()
        return allSessions
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getSessionsForTimeframe(startEpochMilli: Long, endEpochMilli: Long): Flow<List<SleepSessionEntity>> {
        return dao.getAllSessions().map { sessions ->
            sessions.filter {
                it.startTime.toInstant().toEpochMilli() in startEpochMilli..endEpochMilli
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getSessionsSnapshotForTimeframe(startEpochMilli: Long, endEpochMilli: Long): List<SleepSessionEntity> {
        return dao.getAllSessionsSnapshot().filter { session ->
            session.startTime.toInstant().toEpochMilli() in startEpochMilli..endEpochMilli
        }
    }
}