package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.EverymanType
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import com.gntr.domain.service.SleepSessionManager
import java.time.ZonedDateTime
import javax.inject.Inject

class GenerateScheduleUseCase @Inject constructor(
    private val sleepSessionManager: SleepSessionManager
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(everyman: EverymanType, baseWakeUpTime: ZonedDateTime, daysToGenerate: Int = 2) {
        for (dayOffset in 0 until daysToGenerate) {
            val dailyWakeUpTime = baseWakeUpTime.plusDays(dayOffset.toLong())
            val coreDuration = everyman.coreSleepMinutes.toLong()
            val coreStart = dailyWakeUpTime.minusMinutes(coreDuration)

            val coreSession = SleepSession(
                startTime = coreStart,
                endTime = dailyWakeUpTime,
                type = SessionType.CORE,
                status = SessionStatus.SCHEDULED
            )

            sleepSessionManager.scheduleNew(coreSession)
            var previousWakeTime = dailyWakeUpTime

            repeat(everyman.napCount) {
                val napStart = previousWakeTime.plusHours(everyman.napIntervalHours)
                val napEnd = napStart.plusMinutes(everyman.napDurationMinutes.toLong())

                val napSession = SleepSession(
                    startTime = napStart,
                    endTime = napEnd,
                    type = SessionType.NAP,
                    status = SessionStatus.SCHEDULED
                )

                sleepSessionManager.scheduleNew(napSession)
                previousWakeTime = napEnd
            }
        }
    }
}