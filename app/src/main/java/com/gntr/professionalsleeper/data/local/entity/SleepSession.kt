package com.gntr.professionalsleeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val type: SessionType,
    val status: SessionStatus
)