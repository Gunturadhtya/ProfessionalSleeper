package com.gntr.professionalsleeper.presentation.schedule.sectograph

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import com.gntr.professionalsleeper.data.local.entity.CalendarEventWithSource
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.ui.theme.CoreSleepColor
import com.gntr.professionalsleeper.ui.theme.NapSleepColor
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import androidx.core.graphics.toColorInt

object SectographMapper {

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapSleepSessionsToSectors(sessions: List<SleepSession>): List<SectographSector> {
        val sectors = mutableListOf<SectographSector>()

        val innerRadius = 0.6f
        val outerRadius = 0.8f

        for (session in sessions) {
            val color = if (session.type == SessionType.CORE) CoreSleepColor else NapSleepColor
            val startAngle = calculateAngle(session.startTime)
            val endAngle = calculateAngle(session.endTime)

            if (endAngle < startAngle) {
                sectors.add(
                    SectographSector(
                        color = color,
                        outerRadiusRatio = outerRadius,
                        innerRadiusRatio = innerRadius,
                        startAngle = startAngle,
                        sweepAngle = 360f - startAngle
                    )
                )
                sectors.add(
                    SectographSector(
                        color = color,
                        outerRadiusRatio = outerRadius,
                        innerRadiusRatio = innerRadius,
                        startAngle = 0f,
                        sweepAngle = endAngle
                    )
                )
            } else {
                sectors.add(
                    SectographSector(
                        color = color,
                        outerRadiusRatio = outerRadius,
                        innerRadiusRatio = innerRadius,
                        startAngle = startAngle,
                        sweepAngle = endAngle - startAngle
                    )
                )
            }
        }
        return sectors
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapCalendarEventsToSectors(events: List<CalendarEventWithSource>): List<SectographSector> {
        val sectors = mutableListOf<SectographSector>()
        val innerRadius = 0.4f
        val outerRadius = 0.55f

        for (item in events) {
            val event = item.event
            val source = item.source


            val hexColor = source?.colorHex ?: "#5C6370"
            val baseColor = try {
                Color(hexColor.toColorInt())
            } catch (e: Exception) {
                Color.Gray
            }

            val sectorColor = baseColor.copy(alpha = 0.5f)

            val startZdt = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
            val endZdt = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault())

            val startAngle = calculateAngle(startZdt)
            val endAngle = calculateAngle(endZdt)

            if (endAngle < startAngle) {
                sectors.add(SectographSector(sectorColor, outerRadius, innerRadius, startAngle, 360f - startAngle))
                sectors.add(SectographSector(sectorColor, outerRadius, innerRadius, 0f, endAngle))
            } else {
                sectors.add(SectographSector(sectorColor, outerRadius, innerRadius, startAngle, endAngle - startAngle))
            }
        }
        return sectors
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateAngle(time: ZonedDateTime): Float {
        val totalMinutes = (time.hour * 60) + time.minute
        return (totalMinutes / 1440f) * 360f
    }
}