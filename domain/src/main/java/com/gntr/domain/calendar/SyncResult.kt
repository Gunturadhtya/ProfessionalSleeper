package com.gntr.domain.calendar



sealed class SyncResult<out T> {
    data class Success<T>(val data: T) : SyncResult<T>()
    data class Failure(val error: SyncError) : SyncResult<Nothing>()
}