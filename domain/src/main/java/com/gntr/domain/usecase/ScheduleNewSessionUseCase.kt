package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import com.gntr.domain.service.SleepSessionManager
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ScheduleNewSessionUseCase @Inject constructor(
    private val sleepSessionManager: SleepSessionManager
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(startTimeMilli: Long, endTimeMilli: Long, type: SessionType) {
        val session = SleepSession(
            startTime = Instant.ofEpochMilli(startTimeMilli).atZone(ZoneId.systemDefault()),
            endTime = Instant.ofEpochMilli(endTimeMilli).atZone(ZoneId.systemDefault()),
            type = type,
            status = SessionStatus.SCHEDULED
        )

        sleepSessionManager.scheduleNew(session)
    }
}