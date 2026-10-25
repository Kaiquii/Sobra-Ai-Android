package com.example.appfinanceiro.feature.relatorios

import br.com.sobraai.ui.runtime.PresentationModel as ViewModel
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.data.ReportsDataSource
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.NetworkFailureKind
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.core.network.MonthComparisonResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.network.YearlySummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

data class RelatoriosUiState(
    val summaryData: SummaryResponse? = null,
    val categoryData: List<CategoryReportResponse> = emptyList(),
    val chartData: List<ChartReportResponse> = emptyList(),
    val yearlySummary: YearlySummaryResponse? = null,
    val monthComparison: MonthComparisonResponse? = null,
    val isComparisonLoading: Boolean = false,
    val comparisonErrorMessage: String? = null,
    val comparisonNetworkFailure: NetworkFailureKind? = null,
    val isLoading: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val errorMessage: String? = null,
    val reportsNetworkFailure: NetworkFailureKind? = null,
    val isSessionExpired: Boolean = false
)

class RelatoriosViewModel(
    private val repository: ReportsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(RelatoriosUiState())
    val uiState: StateFlow<RelatoriosUiState> = _uiState
    fun loadReports(token: String, month: Int, year: Int, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        launchLatest("reports", listOf(token, month, year, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update {
                it.copy(isLoading = true, isSessionExpired = false,
                    errorMessage = if (it.summaryData != null) it.errorMessage else null,
                    reportsNetworkFailure = if (it.summaryData != null) it.reportsNetworkFailure else null)
            }

            try {
                val summary = repository.getSummary(token, month, year)
                val categories = repository.getReportCategories(token, month, year)
                val chart = repository.getReportChart(token, year)
                val yearly = repository.getYearlySummary(token, year)

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        summaryData = summary,
                        categoryData = categories,
                        chartData = chart,
                        yearlySummary = yearly,
                        errorMessage = null, reportsNetworkFailure = null
                    )
                }
            } catch (e: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        errorMessage = e.userMessageOr("Erro ao carregar relatórios"), reportsNetworkFailure = (e as? NetworkRequestException)?.kind
                    )
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isLoading = false, hasLoadedOnce = true)
                }
            }
        }
    }

    fun loadMonthComparison(
        token: String,
        month: Int,
        year: Int,
        compareMonth: Int,
        compareYear: Int,
        refreshKey: Any? = null,
        reuseRecent: Boolean = false
    ) {
        launchLatest("comparison", listOf(token, month, year, compareMonth, compareYear, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update {
                it.copy(
                    isComparisonLoading = true,
                    comparisonErrorMessage = if (it.monthComparison != null) it.comparisonErrorMessage else null,
                    comparisonNetworkFailure = if (it.monthComparison != null) it.comparisonNetworkFailure else null,
                    isSessionExpired = false
                )
            }

            try {
                val comparison = repository.getMonthComparison(
                    token = token,
                    month = month,
                    year = year,
                    compareMonth = compareMonth,
                    compareYear = compareYear
                )

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(monthComparison = comparison, comparisonErrorMessage = null, comparisonNetworkFailure = null)
                }
            } catch (e: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        comparisonErrorMessage = e.userMessageOr("Erro ao carregar comparativo mensal"), comparisonNetworkFailure = (e as? NetworkRequestException)?.kind
                    )
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isComparisonLoading = false)
                }
            }
        }
    }

    fun clearSessionExpired() {
        _uiState.update {
            it.copy(isSessionExpired = false)
        }
    }
}
