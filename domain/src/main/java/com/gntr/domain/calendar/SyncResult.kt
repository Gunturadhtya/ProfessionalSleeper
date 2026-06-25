package com.gntr.domain.calendar

sealed interface SyncResult<out T>

data class SyncSuccess<out T>(val data: T) : SyncResult<T>

data class SyncFailure(val error: SyncError) : SyncResult<Nothing>
