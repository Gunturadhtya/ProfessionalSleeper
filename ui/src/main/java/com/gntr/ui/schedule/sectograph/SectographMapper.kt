package com.gntr.ui.schedule.sectograph

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.gntr.domain.model.CalendarEventDetail
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import com.gntr.ui.theme.CoreSleepColor
import com.gntr.ui.theme.NapSleepColor
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object SectographMapper {

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapSleepSessionsToSectors(sessions: List<SleepSession>): List<SectographSector> {
        val innerRadius = 0.6f
        val outerRadius = 0.8f

        return sessions
            .filter { it.status != SessionStatus.CANCELLED }
            .flatMap { session ->
                val color = when {
                    session.status == SessionStatus.BLOCKED_BY_EVENT -> Color.Gray.copy(alpha = 0.4f)
                    session.type == SessionType.CORE -> CoreSleepColor
                    else -> NapSleepColor
                }
                buildArcSectors(
                    color = color,
                    outerRadius = outerRadius,
                    innerRadius = innerRadius,
                    startAngle = calculateAngle(session.startTime),
                    endAngle = calculateAngle(session.endTime)
                )
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapCalendarEventsToSectors(events: List<CalendarEventDetail>): List<SectographSector> {
        val innerRadius = 0.4f
        val outerRadius = 0.55f

        return events.flatMap { event ->
            val baseColor = try {
                Color(event.sourceColorHex.toColorInt())
            } catch (e: Exception) {
                Color.Gray
            }
            buildArcSectors(
                color = baseColor.copy(alpha = 0.5f),
                outerRadius = outerRadius,
                innerRadius = innerRadius,
                startAngle = calculateAngle(Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())),
                endAngle = calculateAngle(Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault()))
            )
        }
    }

    private fun buildArcSectors(
        color: Color,
        outerRadius: Float,
        innerRadius: Float,
        startAngle: Float,
        endAngle: Float
    ): List<SectographSector> = when {
        endAngle < startAngle -> listOf(
            SectographSector(color, outerRadius, innerRadius, startAngle, 360f - startAngle),
            SectographSector(color, outerRadius, innerRadius, 0f, endAngle)
        )
        endAngle == startAngle -> listOf(
            SectographSector(color, outerRadius, innerRadius, 0f, 360f)
        )
        else -> listOf(
            SectographSector(color, outerRadius, innerRadius, startAngle, endAngle - startAngle)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateAngle(time: ZonedDateTime): Float {
        val totalMinutes = (time.hour * 60) + time.minute
        return (totalMinutes / 1440f) * 360f
    }
}