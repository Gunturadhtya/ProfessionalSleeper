package com.gntr.professionalsleeper.presentation.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.entity.CalendarEventWithSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarEventUiMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToUiModel(relationalEvent: CalendarEventWithSource): CalendarEventUiModel {
        val event = relationalEvent.event
        val source = relationalEvent.source

        val startZdt = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault())

        return CalendarEventUiModel(
            id = event.id,
            title = event.title,
            timeRange = "${startZdt.format(timeFormat)} – ${endZdt.format(timeFormat)}",
            tagLabel = source?.displayName ?: "Unknown Calendar",
            tagColorHex = source?.colorHex ?: "#5C6370"
        )
    }
}