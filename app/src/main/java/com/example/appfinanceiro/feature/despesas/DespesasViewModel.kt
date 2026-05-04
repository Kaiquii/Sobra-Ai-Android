package com.example.appfinanceiro.feature.despesas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanceiro.core.data.FinanceRepository
import com.example.appfinanceiro.core.network.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DespesasUiState(
    val expensesData: List<Expense> = emptyList(),
    val categoriesMap: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val deleteErrorMessage: String? = null
)

class DespesasViewModel(
    private val repository: FinanceRepository = FinanceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DespesasUiState())
    val uiState: StateFlow<DespesasUiState> = _uiState

    fun loadExpenses(token: String, month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
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
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao carregar despesas", e)
                _uiState.update {
                    it.copy(
                        expensesData = emptyList(),
                        errorMessage = "Erro ao carregar despesas"
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isLoading = false)
                }
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
                _uiState.update {
                    it.copy(deleteSuccessMessage = "Excluído com sucesso!")
                }
                onDeleted()
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao excluir despesa", e)
                _uiState.update {
                    it.copy(deleteErrorMessage = "Erro ao excluir")
                }
            } finally {
                _uiState.update {
                    it.copy(isDeleting = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(deleteSuccessMessage = null, deleteErrorMessage = null)
        }
    }
}
