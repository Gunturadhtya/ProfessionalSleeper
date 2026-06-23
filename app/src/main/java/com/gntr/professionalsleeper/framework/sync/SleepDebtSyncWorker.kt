package com.gntr.professionalsleeper.framework.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gntr.professionalsleeper.data.local.dao.SleepDebtDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SleepDebtSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepDebtDao: SleepDebtDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val unsyncedDebts = sleepDebtDao.getUnsyncedDebts()
        if (unsyncedDebts.isEmpty()) return Result.success()

        return try {
            Timber.i("Menyinkronkan ${unsyncedDebts.size} data SleepDebt ke remote server.")

            val syncedDates = unsyncedDebts.map { it.date }
            sleepDebtDao.markAsSynced(syncedDates)

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Gagal menyinkronkan Sleep Debt.")
            Result.retry()
        }
    }
}