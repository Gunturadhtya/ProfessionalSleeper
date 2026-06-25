package com.gntr.domain.model

import java.time.ZonedDateTime

data class SleepSession(
    val id: Long = 0L,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val type: SessionType,
    val status: SessionStatus,
    val snoozeCount: Int = 0
)