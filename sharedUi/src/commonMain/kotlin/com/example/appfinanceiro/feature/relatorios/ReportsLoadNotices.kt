package com.example.appfinanceiro.feature.relatorios

import com.example.appfinanceiro.core.designsystem.components.*

enum class ReportsLoadSource { Reports, Comparison }
fun reportsLoadNotices(state: RelatoriosUiState): List<LoadNotice<ReportsLoadSource>> = loadNotices(listOf(
    LoadFailure(ReportsLoadSource.Reports, "Relatórios", state.errorMessage, state.reportsNetworkFailure,
        state.summaryData != null || state.categoryData.isNotEmpty() || state.chartData.isNotEmpty() || state.yearlySummary != null,
        state.isLoading),
    LoadFailure(ReportsLoadSource.Comparison, "Comparativo", state.comparisonErrorMessage, state.comparisonNetworkFailure,
        state.monthComparison != null, state.isComparisonLoading)
))
