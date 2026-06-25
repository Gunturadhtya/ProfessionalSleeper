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

class TriggerDebugAlarmUseCase @Inject constructor(
    private val sleepSessionManager: SleepSessionManager
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke() {
        val now = System.currentTimeMillis()
        val debugSession = SleepSession(
            startTime = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()),
            endTime = Instant.ofEpochMilli(now + 5000).atZone(ZoneId.systemDefault()),
            type = SessionType.NAP,
            status = SessionStatus.SCHEDULED
        )

        sleepSessionManager.scheduleNew(debugSession)
    }
}