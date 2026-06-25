package com.gntr.ui.analysis

import com.gntr.domain.model.SleepAnalysisReport

sealed interface AnalysisUiState {

    data object Loading : AnalysisUiState

    data class Success(
        val report: SleepAnalysisReport,
        val adherencePoints: List<Float>,
        val debtBarValues: List<Float>,
        val dateLabels: List<String>,
        val overallScore: String,
        val totalDebtLabel: String,
        val selectedWindow: Int = DEFAULT_WINDOW
    ) : AnalysisUiState

    data class Error(val message: String) : AnalysisUiState

    companion object {
        const val DEFAULT_WINDOW = 7
    }
}