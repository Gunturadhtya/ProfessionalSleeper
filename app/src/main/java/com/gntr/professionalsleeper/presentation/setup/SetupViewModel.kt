package com.gntr.professionalsleeper.presentation.setup

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.entity.EverymanType
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.domain.alarm.IAlarmScheduler
import com.gntr.professionalsleeper.domain.repository.ISleepSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefsRepo: AppPreferencesRepository,
    private val sessionRepository: ISleepSessionRepository,
    private val alarmScheduler: IAlarmScheduler
) : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    fun completeSetup(everyman: EverymanType, coreWakeUpTime: ZonedDateTime) {
        viewModelScope.launch {
            prefsRepo.saveCoreSleepTarget(everyman.coreSleepMinutes)
            prefsRepo.setSetupComplete(true)

            val napDuration = everyman.napDurationMinutes.toLong()
            val coreDuration = everyman.coreSleepMinutes.toLong()

            val coreSession = SleepSession(
                startTime = coreWakeUpTime.minusMinutes(coreDuration),
                endTime = coreWakeUpTime,
                type = SessionType.CORE,
                status = SessionStatus.SCHEDULED
            )
            val coreId = sessionRepository.insertSession(coreSession)
            val coreWithId = coreSession.copy(id = coreId.toInt())
            if (coreWithId.endTime.isAfter(ZonedDateTime.now())) {
                alarmScheduler.scheduleAlarm(coreWithId)
            }

            var nextNapStart = coreWakeUpTime.plusHours(everyman.napIntervalHours)

            repeat(everyman.napCount) {
                val napSession = SleepSession(
                    startTime = nextNapStart,
                    endTime = nextNapStart.plusMinutes(napDuration),
                    type = SessionType.NAP,
                    status = SessionStatus.SCHEDULED
                )
                val napId = sessionRepository.insertSession(napSession)
                val napWithId = napSession.copy(id = napId.toInt())
                if (napWithId.endTime.isAfter(ZonedDateTime.now())) {
                    alarmScheduler.scheduleAlarm(napWithId)
                }

                nextNapStart = nextNapStart.plusHours(everyman.napIntervalHours)
            }
        }
    }
}