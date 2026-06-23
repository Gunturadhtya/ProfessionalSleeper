package com.gntr.professionalsleeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime


@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val type: SessionType,
    val status: SessionStatus,
    val snoozeCount: Int = 0
)