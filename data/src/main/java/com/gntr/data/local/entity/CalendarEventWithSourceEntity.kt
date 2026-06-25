package com.gntr.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CalendarEventWithSourceEntity(
    @Embedded val event: CalendarEventEntity,

    @Relation(
        parentColumn = "sourceId",
        entityColumn = "id"
    )
    val source: CalendarSourceEntity?
)