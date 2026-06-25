package com.gntr.domain.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.EventTime
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
fun CalendarEvent.getAdjustedStartTime(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return when (val time = this.startTime) {
        is EventTime.Exact -> time.epochMilli
        is EventTime.AllDay -> time.date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun CalendarEvent.getAdjustedEndTime(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return when (val time = this.endTime) {
        is EventTime.Exact -> time.epochMilli
        is EventTime.AllDay -> time.date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}