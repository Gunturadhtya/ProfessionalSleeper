package com.gntr.domain.repository

import com.gntr.domain.model.CalendarSource
import kotlinx.coroutines.flow.Flow

interface ICalendarSourceRepository {
    fun getAllSourcesFlow(): Flow<List<CalendarSource>>
    suspend fun getEnabledSources(): List<CalendarSource>
    suspend fun insertSources(sources: List<CalendarSource>)
    suspend fun updateSource(source: CalendarSource)
    suspend fun deleteOrphanedSources(validIds: List<String>)
}