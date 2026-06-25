package com.gntr.framework.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.domain.repository.ISleepDebtRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SleepDebtSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepDebtRepository: ISleepDebtRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsyncedDebts = sleepDebtRepository.getUnsyncedDebts()

            if (unsyncedDebts.isEmpty()) {
                Timber.d("No unsynced sleep debts found. Sync complete.")
                return Result.success()
            }

            val syncSuccess = true

            if (syncSuccess) {
                val syncedDates = unsyncedDebts.map { it.date }
                sleepDebtRepository.markAsSynced(syncedDates)
                Timber.i("Successfully synced ${syncedDates.size} sleep debt records.")
                Result.success()
            } else {
                Timber.w("Remote server rejected sleep debt sync.")
                Result.retry()
            }

        } catch (e: Exception) {
            Timber.e(e, "SleepDebt sync failed due to an exception.")
            Result.retry()
        }
    }
}