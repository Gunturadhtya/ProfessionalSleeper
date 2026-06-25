package com.gntr.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.gntr.domain.model.ScheduleTimeframe
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetScheduleTimeframeUseCase @Inject constructor() {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(daysOffset: Long = 0): ScheduleTimeframe {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        val startMilli = today.atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val endMilli = today.plusDays(daysOffset + 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1

        return ScheduleTimeframe(startMilli, endMilli)
    }
}