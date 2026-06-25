package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.ScheduleTimeframe
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetScheduleTimeframeUseCase @Inject constructor() {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(startDaysOffset: Long, endDaysOffset: Long): ScheduleTimeframe {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        val startMilli = today.plusDays(startDaysOffset)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val endMilli = today.plusDays(endDaysOffset + 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1

        return ScheduleTimeframe(startMilli, endMilli)
    }
}