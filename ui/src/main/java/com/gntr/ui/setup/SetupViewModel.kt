package com.gntr.ui.setup

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.model.EverymanType
import com.gntr.domain.repository.IPreferencesRepository
import com.gntr.domain.usecase.GenerateScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefsRepo: IPreferencesRepository,
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