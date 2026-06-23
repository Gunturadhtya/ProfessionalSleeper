package com.gntr.professionalsleeper.presentation.schedule

import java.time.LocalDate
import java.time.ZonedDateTime

sealed interface ScheduleListItem {
    val sortKey: ZonedDateTime
    val itemKey: String

    data class Session(val session: SleepSessionUiModel, override val sortKey: ZonedDateTime) : ScheduleListItem {
        override val itemKey: String get() = "session:${session.id}"
    }

    data class CalendarEvent(val event: CalendarEventUiModel, override val sortKey: ZonedDateTime) : ScheduleListItem {
        override val itemKey: String get() = "event:${event.id}"
    }

    data class DateHeader(val date: LocalDate, val sessionCount: Int, override val sortKey: ZonedDateTime) : ScheduleListItem {
        override val itemKey: String get() = "header:${date}"
    }
}