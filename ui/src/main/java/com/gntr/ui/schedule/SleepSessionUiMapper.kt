package com.gntr.ui.schedule

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.ui.R
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SleepSessionUiMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToUiModel(
        session: SleepSession,
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now()
    ): SleepSessionUiModel {
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val startStr = session.startTime.format(timeFormat)
        val endStr = session.endTime.format(timeFormat)
        val durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes()

        val typeLabel = if (session.type == SessionType.CORE) {
            context.getString(R.string.session_type_core)
        } else {
            context.getString(R.string.session_type_nap)
        }

        return SleepSessionUiModel(
            id = session.id,
            timeRange = "$startStr – $endStr",
            durationText = context.getString(R.string.session_duration, durationMinutes),
            typeLabel = typeLabel,
            type = session.type,
            status = session.status,
            isOngoing = !session.startTime.isAfter(now) && session.endTime.isAfter(now),
            isPast = !session.endTime.isAfter(now),
            startTimeMillis = session.startTime.toInstant().toEpochMilli(),
            endTimeMillis = session.endTime.toInstant().toEpochMilli()
        )
    }
}