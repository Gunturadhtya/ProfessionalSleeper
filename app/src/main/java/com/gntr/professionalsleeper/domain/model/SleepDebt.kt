package com.gntr.professionalsleeper.domain.model

data class SleepDebt(
    val date: String,
    val debtMinutes: Int,
    val isSynced: Boolean = false
)