package com.gntr.professionalsleeper.domain.usecase

import com.gntr.professionalsleeper.data.local.dao.CalendarSourceDao
import com.gntr.professionalsleeper.domain.calendar.ICalendarSyncService
import javax.inject.Inject

class ReconcileCalendarsUseCase @Inject constructor(
    private val calendarSyncService: ICalendarSyncService,
    private val calendarSourceDao: CalendarSourceDao
) {
    suspend operator fun invoke(accessToken: String) {
        val remoteResult = calendarSyncService.fetchAvailableCalendars(accessToken)

        remoteResult.onSuccess { remoteSources ->
            val remoteIds = remoteSources.map { it.id }

            calendarSourceDao.deleteOrphanedSources(remoteIds)
            calendarSourceDao.insertSources(remoteSources)
        }
    }
}