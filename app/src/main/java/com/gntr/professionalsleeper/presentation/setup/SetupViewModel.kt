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
import com.gntr.professionalsleeper.domain.usecase.GenerateScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefsRepo: AppPreferencesRepository,
    private val generateScheduleUseCase: GenerateScheduleUseCase
) : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    fun completeSetup(everyman: EverymanType, coreWakeUpTime: ZonedDateTime) {
        viewModelScope.launch {
            prefsRepo.saveCoreSleepTarget(everyman.coreSleepMinutes)
            prefsRepo.saveEverymanType(everyman.name)
            prefsRepo.saveWakeUpTime(coreWakeUpTime.hour, coreWakeUpTime.minute)

            generateScheduleUseCase(everyman, coreWakeUpTime, daysToGenerate = 3)

            prefsRepo.setSetupComplete(true)
        }
    }
}