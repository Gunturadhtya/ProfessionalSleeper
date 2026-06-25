package com.gntr.domain.model

data class SleepAnalysisReport(
    val dailyMetrics: List<DailySleepMetrics>
) {

    fun rollingAdherenceAverage(windowDays: Int = 7): List<Float> {
        require(windowDays > 0) { "windowDays must be positive, got $windowDays" }
        return dailyMetrics.mapIndexed { index, _ ->
            val from = maxOf(0, index - windowDays + 1)
            val window = dailyMetrics.subList(from, index + 1)
            if (window.isEmpty()) 0f
            else window.map { it.adherenceScore }.average().toFloat()
        }
    }

    fun rollingActualMinutesAverage(windowDays: Int = 7): List<Float> {
        require(windowDays > 0) { "windowDays must be positive, got $windowDays" }
        return List(dailyMetrics.size) { index ->
            val from = maxOf(0, index - windowDays + 1)
            val window = dailyMetrics.subList(from, index + 1)
            if (window.isEmpty()) 0f
            else window.map { it.totalActualMinutes }.average().toFloat()
        }
    }

    val overallAdherenceScore: Float
        get() = if (dailyMetrics.isEmpty()) 0f
        else dailyMetrics.map { it.adherenceScore }.average().toFloat()

    val totalSleepDebt: Int
        get() = dailyMetrics.sumOf { it.sleepDebtAccumulated }
}