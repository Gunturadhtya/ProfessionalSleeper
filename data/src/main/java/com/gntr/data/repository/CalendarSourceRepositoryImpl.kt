package com.gntr.data.repository


import com.gntr.data.local.dao.CalendarSourceDao
import com.gntr.data.local.mapper.toDomain
import com.gntr.data.local.mapper.toEntity
import com.gntr.domain.model.CalendarSource
import com.gntr.domain.repository.ICalendarSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CalendarSourceRepositoryImpl @Inject constructor(
    private val dao: CalendarSourceDao
) : ICalendarSourceRepository {

    override fun getAllSourcesFlow(): Flow<List<CalendarSource>> {
        return dao.getAllSourcesFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEnabledSources(): List<CalendarSource> {
        return dao.getEnabledSources().map { it.toDomain() }
    }

    override suspend fun insertSources(sources: List<CalendarSource>) {
        dao.insertSources(sources.map { it.toEntity() })
    }

    override suspend fun updateSource(source: CalendarSource) {
        dao.updateSource(source.toEntity())
    }

    override suspend fun deleteOrphanedSources(validIds: List<String>) {
        dao.deleteOrphanedSources(validIds)
    }

    override suspend fun disableSource(sourceId: String) {
        dao.disableSource(sourceId)
    }
}