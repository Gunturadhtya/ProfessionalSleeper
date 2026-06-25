package com.gntr.data.local.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.data.local.entity.CalendarEventEntity
import com.gntr.data.local.entity.CalendarEventWithSourceEntity
import com.gntr.data.local.entity.CalendarSourceEntity
import com.gntr.data.local.entity.SleepDebtEntity
import com.gntr.data.local.entity.SleepSessionEntity
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.CalendarSource
import com.gntr.domain.model.EventTime
import com.gntr.domain.model.SleepDebt
import com.gntr.domain.model.SleepSession
import java.time.Instant
import java.time.ZoneOffset

fun SleepSessionEntity.toDomain(): SleepSession {
    return SleepSession(
        id = this.id,
        startTime = this.startTime,
        endTime = this.endTime,
        type = this.type,
        status = this.status,
        snoozeCount = this.snoozeCount
    )
}

fun SleepSession.toEntity(): SleepSessionEntity {
    return SleepSessionEntity(
        id = this.id,
        startTime = this.startTime,
        endTime = this.endTime,
        type = this.type,
        status = this.status,
        snoozeCount = this.snoozeCount
    )
}

fun SleepDebtEntity.toDomain(): SleepDebt {
    return SleepDebt(
        date = this.date,
        debtMinutes = this.debtMinutes,
        isSynced = this.isSynced
    )
}

fun SleepDebt.toEntity(): SleepDebtEntity {
    return SleepDebtEntity(
        date = this.date,
        debtMinutes = this.debtMinutes,
        isSynced = this.isSynced
    )
}

fun CalendarSourceEntity.toDomain(): CalendarSource {
    return CalendarSource(
        id = this.id,
        displayName = this.displayName,
        colorHex = this.colorHex,
        isEnabled = this.isEnabled
    )
}

fun CalendarSource.toEntity(): CalendarSourceEntity {
    return CalendarSourceEntity(
        id = this.id,
        displayName = this.displayName,
        colorHex = this.colorHex,
        isEnabled = this.isEnabled
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun CalendarEventEntity.toDomain(): CalendarEvent {
    val domainStartTime = if (this.isAllDay) {
        val date = Instant.ofEpochMilli(this.startTime).atZone(ZoneOffset.UTC).toLocalDate()
        EventTime.AllDay(date)
    } else {
        EventTime.Exact(this.startTime)
    }

    val domainEndTime = if (this.isAllDay) {
        val date = Instant.ofEpochMilli(this.endTime).atZone(ZoneOffset.UTC).toLocalDate()
        EventTime.AllDay(date)
    } else {
        EventTime.Exact(this.endTime)
    }

    return CalendarEvent(
        id = this.id,
        sourceId = this.sourceId,
        title = this.title,
        startTime = domainStartTime,
        endTime = domainEndTime
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun CalendarEvent.toEntity(): CalendarEventEntity {
    val isAllDay = this.startTime is EventTime.AllDay

    val entityStartTime = when (val time = this.startTime) {
        is EventTime.Exact -> time.epochMilli
        is EventTime.AllDay -> time.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    val entityEndTime = when (val time = this.endTime) {
        is EventTime.Exact -> time.epochMilli
        is EventTime.AllDay -> time.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    return CalendarEventEntity(
        id = this.id,
        sourceId = this.sourceId,
        title = this.title,
        startTime = entityStartTime,
        endTime = entityEndTime,
        isAllDay = isAllDay
    )
}

fun CalendarEventWithSourceEntity.toDomain(): CalendarEvent {
    return this.event.toDomain()
}