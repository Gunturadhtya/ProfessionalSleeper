package com.gntr.domain.model

data class SleepDebt(
    val date: String,
    val debtMinutes: Int,
    val isSynced: Boolean = false
)