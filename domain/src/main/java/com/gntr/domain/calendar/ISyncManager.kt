package com.gntr.domain.calendar

import kotlinx.coroutines.flow.Flow

interface ISyncManager {
    val isSyncing: Flow<Boolean>
    fun triggerCalendarSync()
}