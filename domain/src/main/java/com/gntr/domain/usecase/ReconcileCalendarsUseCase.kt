package com.gntr.domain.usecase

import com.gntr.domain.calendar.ICalendarSyncService
import com.gntr.domain.calendar.SyncResult
import com.gntr.domain.calendar.SyncSuccess
import com.gntr.domain.repository.ICalendarSourceRepository
import javax.inject.Inject

class ReconcileCalendarsUseCase @Inject constructor(
    private val calendarSyncService: ICalendarSyncService,
    private val calendarSourceRepository: ICalendarSourceRepository
) {
    suspend operator fun invoke(accessToken: String) {
        val remoteResult = calendarSyncService.fetchAvailableCalendars(accessToken)

        if (remoteResult is SyncSuccess) {
            val remoteSources = remoteResult.data
            val remoteIds = remoteSources.map { it.id }

            calendarSourceRepository.deleteOrphanedSources(remoteIds)
            calendarSourceRepository.insertSources(remoteSources)
        }
    }
}