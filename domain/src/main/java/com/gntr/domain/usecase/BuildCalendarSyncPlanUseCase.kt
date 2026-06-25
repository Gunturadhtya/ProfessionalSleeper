package com.gntr.domain.usecase

import com.gntr.domain.calendar.SyncError
import com.gntr.domain.calendar.SyncFailure
import com.gntr.domain.calendar.SyncPlan
import com.gntr.domain.calendar.SyncResult
import com.gntr.domain.calendar.SyncSuccess
import com.gntr.domain.model.CalendarEvent
import javax.inject.Inject

class BuildCalendarSyncPlanUseCase @Inject constructor() {

    operator fun invoke(
        resultsBySourceId: Map<String, SyncResult<List<CalendarEvent>>>,
        localEventIdsBySourceId: Map<String, List<String>>
    ): SyncPlan {
        val eventsToInsert = mutableListOf<CalendarEvent>()
        val sourceIdsToDisable = mutableListOf<String>()
        val eventIdsToDelete = mutableListOf<String>()
        var requiresRetry = false

        for ((sourceId, result) in resultsBySourceId) {
            when (result) {
                is SyncSuccess -> {
                    val events = result.data
                    eventsToInsert.addAll(events)

                    val remoteIds = events.map { it.id }.toSet()
                    val localIds = localEventIdsBySourceId[sourceId].orEmpty()
                    eventIdsToDelete.addAll(localIds.filterNot { remoteIds.contains(it) })
                }
                is SyncFailure -> when (val error = result.error) {
                    is SyncError.Transient -> requiresRetry = true
                    is SyncError.PermanentAuthFailure -> {
                        sourceIdsToDisable.add(sourceId)
                        eventIdsToDelete.addAll(localEventIdsBySourceId[sourceId].orEmpty())
                    }
                }
            }
        }

        return SyncPlan(eventIdsToDelete, eventsToInsert, sourceIdsToDisable, requiresRetry)
    }
}