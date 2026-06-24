package com.gntr.professionalsleeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_debts")
data class SleepDebtEntity(
    @PrimaryKey val date: String,
    val debtMinutes: Int,
    val isSynced: Boolean = false
)