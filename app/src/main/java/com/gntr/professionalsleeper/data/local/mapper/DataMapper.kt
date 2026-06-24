package com.gntr.professionalsleeper.data.local.mapper

import com.gntr.professionalsleeper.data.local.entity.CalendarEventEntity
import com.gntr.professionalsleeper.data.local.entity.CalendarEventWithSourceEntity
import com.gntr.professionalsleeper.data.local.entity.CalendarSourceEntity
import com.gntr.professionalsleeper.data.local.entity.SleepDebtEntity
import com.gntr.professionalsleeper.data.local.entity.SleepSessionEntity
import com.gntr.professionalsleeper.domain.model.CalendarEvent
import com.gntr.professionalsleeper.domain.model.CalendarSource
import com.gntr.professionalsleeper.domain.model.SleepDebt
import com.gntr.professionalsleeper.domain.model.SleepSession

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

fun CalendarEventEntity.toDomain(): CalendarEvent {
    return CalendarEvent(
        id = this.id,
        sourceId = this.sourceId,
        title = this.title,
        startTime = this.startTime,
        endTime = this.endTime
    )
}

fun CalendarEvent.toEntity(): CalendarEventEntity {
    return CalendarEventEntity(
        id = this.id,
        sourceId = this.sourceId,
        title = this.title,
        startTime = this.startTime,
        endTime = this.endTime
    )
}

fun CalendarEventWithSourceEntity.toDomain(): CalendarEvent {
    return this.event.toDomain()
}