package com.gntr.framework.sync

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gntr.domain.calendar.ISyncManager
import com.gntr.framework.calendar.CalendarSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SyncManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ISyncManager {

    private val workManager = WorkManager.getInstance(context)

    override val isSyncing: Flow<Boolean> = workManager
        .getWorkInfosForUniqueWorkLiveData("ImmediateCalendarSync")
        .asFlow()
        .map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }

    override fun triggerCalendarSync() {
        val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "ImmediateCalendarSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}