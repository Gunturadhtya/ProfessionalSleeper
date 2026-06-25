package com.gntr.domain.usecase

import com.gntr.domain.calendar.SyncError
import com.gntr.domain.calendar.SyncPlan
import com.gntr.domain.calendar.SyncResult
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
                is SyncResult.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val events = result.data as List<CalendarEvent>
                    eventsToInsert.addAll(events)

                    val remoteIds = events.map { it.id }.toSet()
                    val localIds = localEventIdsBySourceId[sourceId].orEmpty()
                    eventIdsToDelete.addAll(localIds.filterNot { remoteIds.contains(it) })
                }
                is SyncResult.Failure -> when (val error = result.error) {
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