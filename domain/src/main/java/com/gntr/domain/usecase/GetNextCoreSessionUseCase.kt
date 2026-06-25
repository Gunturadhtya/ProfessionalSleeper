package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import java.time.ZonedDateTime
import javax.inject.Inject

class GetNextCoreSessionUseCase @Inject constructor() {
    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(allSessions: List<SleepSession>, now: ZonedDateTime): SleepSession? {
        val zoneId = now.zone
        val today = now.toLocalDate()

        val validSessions = allSessions.filter { it.status != SessionStatus.CANCELLED }

        val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfToday = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        val endOfTomorrow = today.plusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val todayFutureCore = validSessions.firstOrNull { session ->
            session.type == SessionType.CORE &&
                    session.endTime.toInstant().toEpochMilli() in startOfToday..endOfToday &&
                    session.endTime.isAfter(now)
        }
        if (todayFutureCore != null) return todayFutureCore

        val nextFutureCore = validSessions
            .filter { session ->
                session.type == SessionType.CORE &&
                        session.endTime.toInstant().toEpochMilli() in (endOfToday + 1)..endOfTomorrow
            }
            .minByOrNull { it.startTime.toInstant().toEpochMilli() }

        if (nextFutureCore != null) return nextFutureCore

        return validSessions
            .filter { session ->
                session.type == SessionType.CORE &&
                        session.endTime.toInstant().toEpochMilli() in startOfToday..endOfToday
            }
            .maxByOrNull { it.endTime.toInstant().toEpochMilli() }
    }
}