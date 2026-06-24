package com.gntr.professionalsleeper.presentation.schedule

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.R
import com.gntr.professionalsleeper.domain.model.SessionStatus
import com.gntr.professionalsleeper.domain.model.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSessionEntity
import com.gntr.professionalsleeper.ui.theme.CoreSleepColor
import com.gntr.professionalsleeper.ui.theme.NapSleepColor
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SleepSessionUiMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToUiModel(
        session: SleepSessionEntity,
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now()
    ): SleepSessionUiModel {
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val startStr = session.startTime.format(timeFormat)
        val endStr = session.endTime.format(timeFormat)

        val durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes()

        val isUpcoming = session.startTime.isAfter(now)
        val isOngoing = !session.startTime.isAfter(now) && session.endTime.isAfter(now)

        val accentColor = if (session.type == SessionType.CORE) CoreSleepColor else NapSleepColor
        val typeLabel = if (session.type == SessionType.CORE) {
            context.getString(R.string.session_type_core)
        } else {
            context.getString(R.string.session_type_nap)
        }

        val (statusLabel, bgColor, textColor) = when {
            isOngoing -> Triple(
                context.getString(R.string.status_ongoing),
                accentColor.copy(alpha = 0.15f),
                accentColor
            )
            isUpcoming && session.status == SessionStatus.SCHEDULED -> Triple(
                context.getString(R.string.status_upcoming),
                androidx.compose.ui.graphics.Color(0xFFEADDFF),
                androidx.compose.ui.graphics.Color(0xFF21005D)
            )
            session.status == SessionStatus.COMPLETED -> Triple(
                context.getString(R.string.status_done),
                androidx.compose.ui.graphics.Color(0xFFE7E0EC),
                androidx.compose.ui.graphics.Color(0xFF49454F)
            )
            else -> Triple(
                session.status.name,
                androidx.compose.ui.graphics.Color(0xFFE7E0EC),
                androidx.compose.ui.graphics.Color(0xFF49454F)
            )
        }

        return SleepSessionUiModel(
            id = session.id,
            timeRange = "$startStr – $endStr",
            durationText = context.getString(R.string.session_duration, durationMinutes),
            typeLabel = typeLabel,
            accentColor = accentColor,
            statusLabel = statusLabel,
            statusBgColor = bgColor,
            statusTextColor = textColor,
            type = session.type,
            status = session.status,
            startTimeMillis = session.startTime.toInstant().toEpochMilli(),
            endTimeMillis = session.endTime.toInstant().toEpochMilli()
        )
    }
}