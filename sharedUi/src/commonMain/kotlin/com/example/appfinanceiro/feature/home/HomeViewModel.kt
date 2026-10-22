package com.example.appfinanceiro.feature.home

import br.com.sobraai.ui.runtime.PresentationModel as ViewModel
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.data.HomeDataSource
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.SummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

data class HomeUiState(
    val summaryData: SummaryResponse? = null,
    val expensesData: List<Expense> = emptyList(),
    val categoriesMap: Map<Int, String> = emptyMap(),
    val incomesData: List<Income> = emptyList(),
    val isSummaryLoading: Boolean = false,
    val isIncomesLoading: Boolean = false,
    val isExpensesLoading: Boolean = false,
    val hasLoadedSummary: Boolean = false,
    val hasLoadedIncomes: Boolean = false,
    val hasLoadedExpenses: Boolean = false,
    val summaryError: String? = null,
    val incomesError: String? = null,
    val expensesError: String? = null,
    val isSessionExpired: Boolean = false
)

class HomeViewModel(
    private val repository: HomeDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun loadAll(token: String, month: Int, year: Int, paymentStatus: String? = null) {
        loadSummary(token, month, year)
        loadIncomes(token, month, year)
        loadExpenses(token, month, year, paymentStatus)
    }

    fun loadSummary(token: String, month: Int, year: Int) {
        launchLatest("summary") {
            _uiState.update {
                it.copy(isSummaryLoading = true, isSessionExpired = false)
            }

            try {
                val summary = repository.getSummary(token, month, year)
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(summaryData = summary, summaryError = null)
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
                    it.copy(summaryError = e.userMessageOr("Não foi possível carregar o resumo financeiro."))
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isSummaryLoading = false, hasLoadedSummary = true)
                }
            }
        }
    }

    fun loadIncomes(token: String, month: Int, year: Int) {
        launchLatest("incomes") {
            _uiState.update {
                it.copy(isIncomesLoading = true, isSessionExpired = false)
            }

            try {
                val response = repository.getIncomes(token, month, year)

                val totals = calculateIncomeTotals(response.incomes)

                currentCoroutineContext().ensureActive()
                _uiState.update { state ->
                    state.copy(
                        incomesData = response.incomes,
                        summaryData = state.summaryData?.copy(
                            salario = totals.salary,
                            adiantamento = totals.advance,
                            renda_extra_amt = totals.extraIncome
                        ),
                        incomesError = null
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
                    it.copy(incomesError = e.userMessageOr("Não foi possível carregar as rendas."))
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isIncomesLoading = false, hasLoadedIncomes = true)
                }
            }
        }
    }

    fun loadExpenses(token: String, month: Int, year: Int, paymentStatus: String? = null) {
        launchLatest("expenses") {
            _uiState.update {
                it.copy(isExpensesLoading = true, isSessionExpired = false)
            }

            try {
                val categories = repository.getCategories(token)
                val expenses = repository.getEffectiveExpenses(token, month, year, paymentStatus)

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        categoriesMap = categories.categories.associate { category ->
                            category.id to category.name
                        },
                        expensesData = expenses.expenses,
                        expensesError = null
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
                    it.copy(expensesError = e.userMessageOr("Não foi possível carregar as despesas."))
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isExpensesLoading = false, hasLoadedExpenses = true)
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
