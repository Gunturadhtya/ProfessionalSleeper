package com.gntr.professionalsleeper.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.entity.EverymanType
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import java.time.ZonedDateTime
import javax.inject.Inject

class GenerateScheduleUseCase @Inject constructor(
    private val sessionRepository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(everyman: EverymanType, baseWakeUpTime: ZonedDateTime, daysToGenerate: Int = 2) {
        val now = ZonedDateTime.now()
        var validWakeUpTime = baseWakeUpTime

        if (validWakeUpTime.minusMinutes(everyman.coreSleepMinutes.toLong()).isBefore(now)) {
            validWakeUpTime = validWakeUpTime.plusDays(1)
        }

        for (dayOffset in 0 until daysToGenerate) {
            val dailyWakeUpTime = validWakeUpTime.plusDays(dayOffset.toLong())
            val coreDuration = everyman.coreSleepMinutes.toLong()
            val coreStart = dailyWakeUpTime.minusMinutes(coreDuration)

            val coreSession = SleepSession(
                startTime = coreStart,
                endTime = dailyWakeUpTime,
                type = SessionType.CORE,
                status = SessionStatus.SCHEDULED
            )
            val coreId = sessionRepository.insertSession(coreSession)
            val coreWithId = coreSession.copy(id = coreId)

            if (coreWithId.endTime.isAfter(now)) {
                alarmScheduler.scheduleAlarm(coreWithId)
            }

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
                val napId = sessionRepository.insertSession(napSession)
                val napWithId = napSession.copy(id = napId)

                if (napWithId.endTime.isAfter(now)) {
                    alarmScheduler.scheduleAlarm(napWithId)
                }

                previousWakeTime = napEnd
            }
        }
    }
}