package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.supportsAdvance

import br.com.sobraai.ui.runtime.PresentationModel as ViewModel
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.data.ExpensesDataSource
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.NetworkFailureKind
import com.example.appfinanceiro.core.network.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

data class DespesasUiState(
    val expensesData: List<Expense> = emptyList(),
    val effectiveExpensesData: List<Expense> = emptyList(),
    val categoriesMap: Map<Int, String> = emptyMap(),
    val categoriesError: String? = null,
    val categoriesNetworkFailure: NetworkFailureKind? = null,
    val expensesNetworkFailure: NetworkFailureKind? = null,
    val isCategoriesLoading: Boolean = false,
    val isLoading: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val isDeleting: Boolean = false,
    val isUpdatingPaymentStatus: Boolean = false,
    val isUpdatingAdvanceStatus: Boolean = false,
    val errorMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val deleteErrorMessage: String? = null,
    val paymentStatusSuccessMessage: String? = null,
    val paymentStatusErrorMessage: String? = null,
    val advanceStatusSuccessMessage: String? = null,
    val advanceStatusErrorMessage: String? = null,
    val isSessionExpired: Boolean = false
)

class DespesasViewModel(
    private val repository: ExpensesDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(DespesasUiState())
    val uiState: StateFlow<DespesasUiState> = _uiState

    fun loadExpenses(token: String, month: Int, year: Int, paymentStatus: String? = null, refreshKey: Any? = null, reuseRecent: Boolean = false) {
        launchLatest("expenses", listOf(token, month, year, paymentStatus, refreshKey), reuseRecent = reuseRecent) {
            _uiState.update {
                it.copy(isLoading = true, isSessionExpired = false,
                    errorMessage = if (it.expensesData.isNotEmpty() || it.effectiveExpensesData.isNotEmpty()) it.errorMessage else null,
                    expensesNetworkFailure = if (it.expensesData.isNotEmpty() || it.effectiveExpensesData.isNotEmpty()) it.expensesNetworkFailure else null)
            }

            try {
                val expenses = repository.getExpenses(token, month, year, paymentStatus)
                val effectiveExpenses = repository.getEffectiveExpenses(token, month, year, paymentStatus)

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        expensesData = expenses.expenses,
                        effectiveExpensesData = effectiveExpenses.expenses,
                        errorMessage = null,
                        expensesNetworkFailure = null
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
                        errorMessage = e.userMessageOr("Erro ao carregar despesas"),
                        expensesNetworkFailure = (e as? NetworkRequestException)?.kind
                    )
                }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update {
                    it.copy(isLoading = false, hasLoadedOnce = true)
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
                    categoriesError = null,
                    categoriesNetworkFailure = null
                ) }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(isSessionExpired = true) }
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(categoriesError = error.userMessageOr("Não foi possível carregar as categorias."),
                    categoriesNetworkFailure = (error as? NetworkRequestException)?.kind) }
            } finally {
                if (currentCoroutineContext().isActive) _uiState.update { it.copy(isCategoriesLoading = false) }
            }
        }
    }

    fun deleteExpense(
        token: String,
        expenseId: Int,
        deleteFuture: Boolean?,
        onDeleted: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    deleteSuccessMessage = null,
                    deleteErrorMessage = null
                )
            }

            try {
                repository.deleteExpense(token, expenseId, deleteFuture)
                FinanceDataChanges.notifyExpenseChanged()
                _uiState.update {
                    it.copy(deleteSuccessMessage = "Excluído com sucesso!")
                }
                onDeleted()
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                _uiState.update {
                    it.copy(deleteErrorMessage = e.userMessageOr("Erro ao excluir"))
                }
            } finally {
                _uiState.update {
                    it.copy(isDeleting = false)
                }
            }
        }
    }

    fun updateExpensePaymentStatus(
        token: String,
        expense: Expense,
        isPaid: Boolean,
        onUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdatingPaymentStatus = true,
                    paymentStatusSuccessMessage = null,
                    paymentStatusErrorMessage = null
                )
            }

            try {
                val response = repository.updateExpensePaymentStatus(token, expense.id, isPaid)
                FinanceDataChanges.notifyExpenseChanged()
                _uiState.update { state ->
                    state.copy(
                        expensesData = state.expensesData.map { currentExpense ->
                            if (currentExpense.id == expense.id) response.expense else currentExpense
                        },
                        paymentStatusSuccessMessage = if (isPaid) "Despesa marcada como paga." else "Despesa marcada como pendente."
                    )
                }
                onUpdated()
            } catch (e: SessionExpiredException) {
                _uiState.update { it.copy(isSessionExpired = true) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                _uiState.update {
                    it.copy(
                        paymentStatusErrorMessage = e.userMessageOr(
                            "Não foi possível atualizar o status de pagamento."
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isUpdatingPaymentStatus = false) }
            }
        }
    }

    fun updateAdvanceStatus(
        token: String,
        expense: Expense,
        isAdvanced: Boolean,
        advancedAt: String?,
        onUpdated: () -> Unit
    ) {
        if (!expense.supportsAdvance) {
            _uiState.update { it.copy(
                advanceStatusSuccessMessage = null,
                advanceStatusErrorMessage = "Somente despesas únicas ou parceladas podem ser adiantadas."
            ) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdatingAdvanceStatus = true,
                    advanceStatusSuccessMessage = null,
                    advanceStatusErrorMessage = null
                )
            }

            try {
                val response = repository.updateAdvanceStatus(
                    token = token,
                    id = expense.id,
                    isAdvanced = isAdvanced,
                    advancedAt = advancedAt
                )
                FinanceDataChanges.notifyExpenseChanged()
                _uiState.update { state ->
                    state.copy(
                        expensesData = state.expensesData.replaceExpense(response.expense),
                        effectiveExpensesData = state.effectiveExpensesData.replaceExpense(response.expense),
                        advanceStatusSuccessMessage = "Adiantamento atualizado."
                    )
                }
                onUpdated()
            } catch (e: SessionExpiredException) {
                _uiState.update { it.copy(isSessionExpired = true) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                _uiState.update {
                    it.copy(
                        advanceStatusErrorMessage = e.userMessageOr(
                            "Não foi possível atualizar o adiantamento."
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isUpdatingAdvanceStatus = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                deleteSuccessMessage = null,
                deleteErrorMessage = null,
                paymentStatusSuccessMessage = null,
                paymentStatusErrorMessage = null,
                advanceStatusSuccessMessage = null,
                advanceStatusErrorMessage = null
            )
        }
    }

    fun clearSessionExpired() {
        _uiState.update {
            it.copy(isSessionExpired = false)
        }
    }
}

private fun List<Expense>.replaceExpense(updated: Expense): List<Expense> =
    map { current -> if (current.id == updated.id) updated else current }
