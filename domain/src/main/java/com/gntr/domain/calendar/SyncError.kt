package com.gntr.domain.calendar

sealed interface SyncError {
    data class Transient(val exception: Throwable) : SyncError

    data class PermanentAuthFailure(val statusCode: Int, val message: String) : SyncError
}