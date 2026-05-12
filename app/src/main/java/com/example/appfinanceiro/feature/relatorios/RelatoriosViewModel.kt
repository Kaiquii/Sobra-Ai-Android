package com.example.appfinanceiro.feature.relatorios

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanceiro.core.data.FinanceRepository
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.network.YearlySummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RelatoriosUiState(
    val summaryData: SummaryResponse? = null,
    val categoryData: List<CategoryReportResponse> = emptyList(),
    val chartData: List<ChartReportResponse> = emptyList(),
    val yearlySummary: YearlySummaryResponse? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false
)

class RelatoriosViewModel(
    private val repository: FinanceRepository = FinanceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RelatoriosUiState())
    val uiState: StateFlow<RelatoriosUiState> = _uiState

    fun loadReports(token: String, month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isSessionExpired = false)
            }

            try {
                val summary = repository.getSummary(token, month, year)
                val categories = repository.getReportCategories(token, month, year)
                val chart = repository.getReportChart(token, year)
                val yearly = repository.getYearlySummary(token, year)

                _uiState.update {
                    it.copy(
                        summaryData = summary,
                        categoryData = categories,
                        chartData = chart,
                        yearlySummary = yearly
                    )
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao carregar relatórios", e)
                _uiState.update {
                    it.copy(
                        summaryData = null,
                        categoryData = emptyList(),
                        chartData = emptyList(),
                        yearlySummary = null,
                        errorMessage = "Erro ao carregar relatórios"
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isLoading = false)
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
