package com.gntr.ui.analysis

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gntr.domain.model.ScheduleTimeframe
import com.gntr.domain.model.SleepAnalysisReport
import com.gntr.domain.usecase.GenerateSleepReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val generateSleepReport: GenerateSleepReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadReport(windowDays = AnalysisUiState.DEFAULT_WINDOW)
    }


    fun onWindowSelected(windowDays: Int) {
        val current = _uiState.value
        if (current is AnalysisUiState.Success) {
            _uiState.value = current.copy(
                adherencePoints = current.report.rollingAdherenceAverage(windowDays),
                selectedWindow = windowDays
            )
        } else {
            loadReport(windowDays)
        }
    }

    fun refresh() {
        val window = (_uiState.value as? AnalysisUiState.Success)?.selectedWindow
            ?: AnalysisUiState.DEFAULT_WINDOW
        loadReport(window)
    }


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun loadReport(windowDays: Int) {
        _uiState.value = AnalysisUiState.Loading
        viewModelScope.launch {
            try {
                val timeframe = buildThirtyDayTimeframe()
                val report: SleepAnalysisReport = withContext(Dispatchers.Default) {
                    generateSleepReport(timeframe)
                }
                _uiState.value = mapToSuccess(report, windowDays)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.localizedMessage ?: "Failed to load sleep analysis"
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildThirtyDayTimeframe(): ScheduleTimeframe {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val start = today.minusDays(29).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return ScheduleTimeframe(start, end)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mapToSuccess(report: SleepAnalysisReport, windowDays: Int): AnalysisUiState.Success {
        val adherencePoints = report.rollingAdherenceAverage(windowDays)

        val debtBarValues = report.dailyMetrics.map { it.sleepDebtAccumulated.toFloat() }

        val dateLabels = report.dailyMetrics.map { metrics ->
            metrics.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }

        val overallPct = (report.overallAdherenceScore * 100).toInt()
        val overallScore = "$overallPct%"

        val totalDebt = report.totalSleepDebt
        val totalDebtLabel = when {
            totalDebt > 0 -> "+${totalDebt} min"
            totalDebt < 0 -> "$totalDebt min"
            else -> "0 min"
        }

        return AnalysisUiState.Success(
            report = report,
            adherencePoints = adherencePoints,
            debtBarValues = debtBarValues,
            dateLabels = dateLabels,
            overallScore = overallScore,
            totalDebtLabel = totalDebtLabel,
            selectedWindow = windowDays
        )
    }
}