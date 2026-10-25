package com.example.appfinanceiro.feature.home

import br.com.sobraai.ui.runtime.PresentationModel as ViewModel
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.data.HomeDataSource
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.NetworkFailureKind
import com.example.appfinanceiro.core.data.NetworkRequestException
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
    val categoriesNetworkFailure: NetworkFailureKind? = null,
    val isCategoriesLoading: Boolean = false,
    val incomesData: List<Income> = emptyList(),
    val isSummaryLoading: Boolean = false,
    val isIncomesLoading: Boolean = false,
    val isExpensesLoading: Boolean = false,
    val hasLoadedSummary: Boolean = false,
    val hasLoadedIncomes: Boolean = false,
    val hasLoadedExpenses: Boolean = false,
    val summaryError: String? = null,
    val summaryNetworkFailure: NetworkFailureKind? = null,
    val incomesError: String? = null,
    val incomesNetworkFailure: NetworkFailureKind? = null,
    val expensesError: String? = null,
    val expensesNetworkFailure: NetworkFailureKind? = null,
    val isSessionExpired: Boolean = false
)

class HomeViewModel(
    private val repository: HomeDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun loadAll(token: String, month: Int, year: Int, paymentStatus: String? = null, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        loadSummary(token, month, year, refreshKey, reuseRecent)
        loadIncomes(token, month, year, refreshKey, reuseRecent)
        loadExpenses(token, month, year, paymentStatus, refreshKey, reuseRecent)
    }

    fun loadSummary(token: String, month: Int, year: Int, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        launchLatest("summary", listOf(token, month, year, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update {
                it.copy(isSummaryLoading = true, isSessionExpired = false,
                    summaryError = if (it.summaryData != null) it.summaryError else null,
                    summaryNetworkFailure = if (it.summaryData != null) it.summaryNetworkFailure else null)
            }

            try {
                val summary = repository.getSummary(token, month, year)
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(summaryData = summary, summaryError = null, summaryNetworkFailure = null)
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
                    it.copy(summaryError = e.userMessageOr("Não foi possível carregar o resumo financeiro."), summaryNetworkFailure = (e as? NetworkRequestException)?.kind)
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isSummaryLoading = false, hasLoadedSummary = true)
                }
            }
        }
    }

    fun loadIncomes(token: String, month: Int, year: Int, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        launchLatest("incomes", listOf(token, month, year, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update {
                it.copy(isIncomesLoading = true, isSessionExpired = false,
                    incomesError = if (it.incomesData.isNotEmpty()) it.incomesError else null,
                    incomesNetworkFailure = if (it.incomesData.isNotEmpty()) it.incomesNetworkFailure else null)
            }

            try {
                val response = repository.getIncomes(token, month, year)

                val monthlyIncomes = response.incomes.filter { it.month == month && it.year == year }
                val totals = calculateIncomeTotals(monthlyIncomes)

                currentCoroutineContext().ensureActive()
                _uiState.update { state ->
                    state.copy(
                        incomesData = monthlyIncomes,
                        summaryData = state.summaryData?.let { summary ->
                            if (summary.month == month && summary.year == year) summary.copy(
                                salario = totals.salary,
                                adiantamento = totals.advance,
                                renda_extra_amt = totals.extraIncome,
                                va = totals.va,
                                vr = totals.vr
                            ) else summary
                        },
                        incomesError = null, incomesNetworkFailure = null
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
                    it.copy(incomesError = e.userMessageOr("Não foi possível carregar as rendas."), incomesNetworkFailure = (e as? NetworkRequestException)?.kind)
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isIncomesLoading = false, hasLoadedIncomes = true)
                }
            }
        }
    }

    fun loadExpenses(token: String, month: Int, year: Int, paymentStatus: String? = null, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        launchLatest("expenses", listOf(token, month, year, paymentStatus, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update {
                it.copy(isExpensesLoading = true, isSessionExpired = false,
                    expensesError = if (it.expensesData.isNotEmpty()) it.expensesError else null,
                    expensesNetworkFailure = if (it.expensesData.isNotEmpty()) it.expensesNetworkFailure else null)
            }

            try {
                val expenses = repository.getEffectiveExpenses(token, month, year, paymentStatus)

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        expensesData = expenses.expenses,
                        expensesError = null, expensesNetworkFailure = null
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
                    it.copy(expensesError = e.userMessageOr("Não foi possível carregar as despesas."), expensesNetworkFailure = (e as? NetworkRequestException)?.kind)
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isExpensesLoading = false, hasLoadedExpenses = true)
                }
            }
        }
        loadCategories(token, refreshKey, reuseRecent)
    }

    fun loadCategories(token: String, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        launchLatest("categories", listOf(token, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update { it.copy(isCategoriesLoading = true,
                categoriesError = if (it.categoriesMap.isNotEmpty()) it.categoriesError else null,
                    categoriesNetworkFailure = if (it.categoriesMap.isNotEmpty()) it.categoriesNetworkFailure else null) }
            try {
                val categories = repository.getCategories(token)
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(
                    categoriesMap = categories.categories.associate { category -> category.id to category.name },
                    categoriesError = null, categoriesNetworkFailure = null
                ) }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(isSessionExpired = true) }
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(categoriesError = error.userMessageOr("Não foi possível carregar as categorias."), categoriesNetworkFailure = (error as? NetworkRequestException)?.kind) }
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
