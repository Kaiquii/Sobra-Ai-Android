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
    val categoriesError: String? = null,
    val isCategoriesLoading: Boolean = false,
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

    fun loadAll(token: String, month: Int, year: Int, paymentStatus: String? = null, refreshKey: Any? = null) {
        loadSummary(token, month, year, refreshKey)
        loadIncomes(token, month, year, refreshKey)
        loadExpenses(token, month, year, paymentStatus, refreshKey)
    }

    fun loadSummary(token: String, month: Int, year: Int, refreshKey: Any? = null) {
        launchLatest("summary", listOf(token, month, year, refreshKey)) {
            _uiState.update {
                it.copy(isSummaryLoading = true, isSessionExpired = false,
                    summaryError = if (it.summaryData != null) it.summaryError else null)
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

    fun loadIncomes(token: String, month: Int, year: Int, refreshKey: Any? = null) {
        launchLatest("incomes", listOf(token, month, year, refreshKey)) {
            _uiState.update {
                it.copy(isIncomesLoading = true, isSessionExpired = false,
                    incomesError = if (it.incomesData.isNotEmpty()) it.incomesError else null)
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

    fun loadExpenses(token: String, month: Int, year: Int, paymentStatus: String? = null, refreshKey: Any? = null) {
        launchLatest("expenses", listOf(token, month, year, paymentStatus, refreshKey)) {
            _uiState.update {
                it.copy(isExpensesLoading = true, isSessionExpired = false,
                    expensesError = if (it.expensesData.isNotEmpty()) it.expensesError else null)
            }

            try {
                val expenses = repository.getEffectiveExpenses(token, month, year, paymentStatus)

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
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
        loadCategories(token, refreshKey)
    }

    fun loadCategories(token: String, refreshKey: Any? = null) {
        launchLatest("categories", listOf(token, refreshKey)) {
            _uiState.update { it.copy(isCategoriesLoading = true,
                categoriesError = if (it.categoriesMap.isNotEmpty()) it.categoriesError else null) }
            try {
                val categories = repository.getCategories(token)
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(
                    categoriesMap = categories.categories.associate { category -> category.id to category.name },
                    categoriesError = null
                ) }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(isSessionExpired = true) }
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(categoriesError = error.userMessageOr("Não foi possível carregar as categorias.")) }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update { it.copy(isCategoriesLoading = false) }
            }
        }
    }

    fun clearSessionExpired() {
        _uiState.update {
            it.copy(isSessionExpired = false)
        }
    }
}
