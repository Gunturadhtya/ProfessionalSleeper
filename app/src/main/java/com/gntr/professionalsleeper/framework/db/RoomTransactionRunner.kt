package com.gntr.professionalsleeper.framework.db

import androidx.room.withTransaction
import com.gntr.professionalsleeper.data.local.AppDatabase
import com.gntr.professionalsleeper.domain.repository.ITransactionRunner
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase
) : ITransactionRunner {
    override suspend operator fun <T> invoke(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }
}