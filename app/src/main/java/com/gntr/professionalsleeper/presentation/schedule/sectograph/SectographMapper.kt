package com.gntr.professionalsleeper.presentation.schedule.sectograph

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.professionalsleeper.data.local.entity.SessionType
import com.gntr.professionalsleeper.data.local.entity.SleepSession
import com.gntr.professionalsleeper.ui.theme.CoreSleepColor
import com.gntr.professionalsleeper.ui.theme.NapSleepColor
import java.time.ZonedDateTime

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
    private fun calculateAngle(time: ZonedDateTime): Float {
        val totalMinutes = (time.hour * 60) + time.minute
        return (totalMinutes / 1440f) * 360f
    }
}