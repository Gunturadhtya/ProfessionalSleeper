package com.gntr.ui.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.CalendarEventDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarEventUiMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToUiModel(eventDetail: CalendarEventDetail): CalendarEventUiModel {
        val startZdt = Instant.ofEpochMilli(eventDetail.startTime).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(eventDetail.endTime).atZone(ZoneId.systemDefault())

        return CalendarEventUiModel(
            id = eventDetail.id,
            title = eventDetail.title,
            timeRange = "${startZdt.format(timeFormat)} – ${endZdt.format(timeFormat)}",
            tagLabel = eventDetail.sourceName,
            tagColorHex = eventDetail.sourceColorHex
        )
    }
}