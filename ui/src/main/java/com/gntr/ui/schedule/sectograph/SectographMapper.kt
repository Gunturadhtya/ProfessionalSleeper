package com.gntr.ui.schedule.sectograph

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.core.graphics.toColorInt
import com.gntr.domain.model.CalendarEventDetail
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import com.gntr.domain.model.SleepSession
import com.gntr.ui.theme.CoreSleepColor
import com.gntr.ui.theme.NapSleepColor
import com.gntr.ui.theme.CalendarEventColor
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class SectographColors(
    val coreSleepColor: Color,
    val napSleepColor: Color,
    val blockedColor: Color,
    val calendarFallbackColor: Color,
    val surface: Color,
) {
    companion object {
        val Default = SectographColors(
            coreSleepColor = CoreSleepColor,
            napSleepColor = NapSleepColor,
            blockedColor = CalendarEventColor.copy(alpha = 0.38f),
            calendarFallbackColor = CalendarEventColor,
            surface = Color.White,
        )
    }
}

@Composable
fun rememberSectographColors(): SectographColors {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    return remember(primary, tertiary, onSurfaceVariant, surface) {
        SectographColors(
            coreSleepColor = primary,
            napSleepColor = tertiary,
            blockedColor = onSurfaceVariant.copy(alpha = 0.38f),
            calendarFallbackColor = onSurfaceVariant,
            surface = surface,
        )
    }
}

object SectographMapper {

    @RequiresApi(Build.VERSION_CODES.O)
    fun mapSleepSessionsToSectors(
        sessions: List<SleepSession>,
        colors: SectographColors = SectographColors.Default,
    ): List<SectographSector> {
        val innerRadius = 0.6f
        val outerRadius = 0.8f

        return sessions
            .filter { it.status != SessionStatus.CANCELLED }
            .flatMap { session ->
                val color = when {
                    session.status == SessionStatus.BLOCKED_BY_EVENT -> colors.blockedColor
                    session.type == SessionType.CORE -> colors.coreSleepColor
                    else -> colors.napSleepColor
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
    fun mapCalendarEventsToSectors(
        events: List<CalendarEventDetail>,
        colors: SectographColors = SectographColors.Default,
    ): List<SectographSector> {
        val innerRadius = 0.4f
        val outerRadius = 0.55f

        return events.flatMap { event ->
            val baseColor = try {
                Color(event.sourceColorHex.toColorInt())
                    .copy(alpha = 0.5f)
                    .compositeOver(colors.surface)
            } catch (e: Exception) {
                colors.calendarFallbackColor
            }
            buildArcSectors(
                color = baseColor,
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
        endAngle == startAngle -> emptyList()
        endAngle < startAngle -> listOf(
            SectographSector(color, outerRadius, innerRadius, startAngle, 360f - startAngle),
            SectographSector(color, outerRadius, innerRadius, 0f, endAngle)
        )
        else -> listOf(
            SectographSector(color, outerRadius, innerRadius, startAngle, endAngle - startAngle)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateAngle(time: ZonedDateTime): Float {
        val totalSeconds = (time.hour * 3600) + (time.minute * 60) + time.second
        return (totalSeconds / 86400f) * 360f
    }
}