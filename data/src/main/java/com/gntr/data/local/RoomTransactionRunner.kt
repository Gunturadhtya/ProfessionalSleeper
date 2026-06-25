package com.gntr.data.local

import androidx.room.withTransaction
import com.gntr.domain.repository.ITransactionRunner
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase
) : ITransactionRunner {
    override suspend operator fun <T> invoke(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }
}