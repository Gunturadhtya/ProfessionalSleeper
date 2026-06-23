package com.gntr.professionalsleeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "calendar_sources")
data class CalendarSourceEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val colorHex: String,
    val isEnabled: Boolean = true
)