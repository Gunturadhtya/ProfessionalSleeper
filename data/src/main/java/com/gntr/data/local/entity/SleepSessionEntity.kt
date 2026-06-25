package com.gntr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import java.time.ZonedDateTime


@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val type: SessionType,
    val status: SessionStatus,
    val snoozeCount: Int = 0
)