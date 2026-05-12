package com.example.appfinanceiro.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanceiro.core.data.FinanceRepository
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.SummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val summaryData: SummaryResponse? = null,
    val expensesData: List<Expense> = emptyList(),
    val categoriesMap: Map<Int, String> = emptyMap(),
    val incomesData: List<Income> = emptyList(),
    val isSummaryLoading: Boolean = false,
    val isIncomesLoading: Boolean = false,
    val isExpensesLoading: Boolean = false,
    val summaryError: String? = null,
    val incomesError: String? = null,
    val expensesError: String? = null,
    val isSessionExpired: Boolean = false
)

class HomeViewModel(
    private val repository: FinanceRepository = FinanceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun loadAll(token: String, month: Int, year: Int) {
        loadSummary(token, month, year)
        loadIncomes(token, month, year)
        loadExpenses(token, month, year)
    }

    fun loadSummary(token: String, month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSummaryLoading = true, summaryError = null, isSessionExpired = false)
            }

            try {
                val summary = repository.getSummary(token, month, year)
                _uiState.update {
                    it.copy(summaryData = summary)
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao carregar resumo", e)
                _uiState.update {
                    it.copy(summaryError = "Não foi possível carregar o resumo financeiro.")
                }
            } finally {
                _uiState.update {
                    it.copy(isSummaryLoading = false)
                }
            }
        }
    }

    fun loadIncomes(token: String, month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isIncomesLoading = true, incomesError = null, isSessionExpired = false)
            }

            try {
                val response = repository.getIncomes(token, month, year)

                val salarioFromIncomes = response.incomes
                    .filter {
                        it.source.equals("Salario", ignoreCase = true) ||
                                it.source.equals("Salário", ignoreCase = true)
                    }
                    .sumOf { it.amount }

                val adiantamentoFromIncomes = response.incomes
                    .filter { it.source.equals("Adiantamento", ignoreCase = true) }
                    .sumOf { it.amount }

                _uiState.update { state ->
                    state.copy(
                        incomesData = response.incomes,
                        summaryData = state.summaryData?.copy(
                            salario = salarioFromIncomes,
                            adiantamento = adiantamentoFromIncomes
                        )
                    )
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao carregar rendas", e)
                _uiState.update {
                    it.copy(incomesError = "Não foi possível carregar as rendas.")
                }
            } finally {
                _uiState.update {
                    it.copy(isIncomesLoading = false)
                }
            }
        }
    }

    fun loadExpenses(token: String, month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isExpensesLoading = true, expensesError = null, isSessionExpired = false)
            }

            try {
                val categories = repository.getCategories(token)
                val expenses = repository.getExpenses(token, month, year)

                _uiState.update {
                    it.copy(
                        categoriesMap = categories.categories.associate { category ->
                            category.id to category.name
                        },
                        expensesData = expenses.expenses
                    )
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao carregar despesas", e)
                _uiState.update {
                    it.copy(expensesError = "Não foi possível carregar as despesas.")
                }
            } finally {
                _uiState.update {
                    it.copy(isExpensesLoading = false)
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
