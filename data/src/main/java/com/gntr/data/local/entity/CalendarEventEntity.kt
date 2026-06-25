package com.gntr.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
    foreignKeys = [
        ForeignKey(
            entity = CalendarSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sourceId"])]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean
)