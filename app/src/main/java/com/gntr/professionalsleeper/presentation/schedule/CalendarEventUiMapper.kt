package com.gntr.professionalsleeper.presentation.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.entity.CalendarEventEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarEventUiMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToUiModel(event: CalendarEventEntity): CalendarEventUiModel {
        val startZdt = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault())

        return CalendarEventUiModel(
            id = event.id,
            title = event.title,
            timeRange = "${startZdt.format(timeFormat)} – ${endZdt.format(timeFormat)}"
        )
    }
}