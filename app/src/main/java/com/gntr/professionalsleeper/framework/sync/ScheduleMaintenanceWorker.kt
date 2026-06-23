package com.gntr.professionalsleeper.framework.sync

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.professionalsleeper.data.local.datastore.AppPreferencesRepository
import com.gntr.professionalsleeper.data.local.entity.EverymanType
import com.gntr.professionalsleeper.domain.usecase.GenerateScheduleUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.ZonedDateTime

@HiltWorker
class ScheduleMaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val prefsRepo: AppPreferencesRepository,
    private val generateScheduleUseCase: GenerateScheduleUseCase
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        return try {
            val isSetupComplete = prefsRepo.isSetupCompleteFlow.first()
            if (!isSetupComplete) return Result.success()

            val typeName = prefsRepo.everymanTypeFlow.first() ?: return Result.failure()
            val hour = prefsRepo.wakeUpHourFlow.first() ?: 7
            val minute = prefsRepo.wakeUpMinuteFlow.first() ?: 0

            val type = EverymanType.valueOf(typeName)
            val baseTime = ZonedDateTime.now().with(LocalTime.of(hour, minute))

            generateScheduleUseCase(type, baseTime, daysToGenerate = 3)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}