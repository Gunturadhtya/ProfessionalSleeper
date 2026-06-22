package com.gntr.professionalsleeper.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.dao.SleepSessionDao
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class SleepSessionRepositoryImpl(
    private val dao: SleepSessionDao
) : ISleepSessionRepository {

    override suspend fun insertSession(session: SleepSession): Long {
        return dao.insertSession(session)
    }

    override suspend fun updateSession(session: SleepSession) {
        dao.updateSession(session)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getSessionsForToday(): Flow<List<SleepSession>> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        return dao.getSessionsForDay(startOfDay, endOfDay)
    }
}