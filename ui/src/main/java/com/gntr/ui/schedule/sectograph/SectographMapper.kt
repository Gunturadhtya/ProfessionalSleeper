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
        val sectors = mutableListOf<SectographSector>()
        val innerRadius = 0.6f
        val outerRadius = 0.8f

        for (session in sessions) {
            if (session.status == SessionStatus.CANCELLED) continue

            val color = when {
                session.status == SessionStatus.BLOCKED_BY_EVENT -> Color.Gray.copy(alpha = 0.4f)
                session.type == SessionType.CORE -> CoreSleepColor
                else -> NapSleepColor
            }

            val startAngle = calculateAngle(session.startTime)
            val endAngle = calculateAngle(session.endTime)

            if (endAngle < startAngle) {
                sectors.add(SectographSector(color, outerRadius, innerRadius, startAngle, 360f - startAngle))
                sectors.add(SectographSector(color, outerRadius, innerRadius, 0f, endAngle))
            } else {
                sectors.add(SectographSector(color, outerRadius, innerRadius, startAngle, endAngle - startAngle))
            }
        }
        return sectors
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapCalendarEventsToSectors(events: List<CalendarEventDetail>): List<SectographSector> {
        val sectors = mutableListOf<SectographSector>()
        val innerRadius = 0.4f
        val outerRadius = 0.55f

        for (event in events) {
            val baseColor = try {
                Color(event.sourceColorHex.toColorInt())
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