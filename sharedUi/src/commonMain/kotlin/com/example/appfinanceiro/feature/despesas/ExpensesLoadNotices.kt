package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.designsystem.components.LoadFailure
import com.example.appfinanceiro.core.designsystem.components.loadNotices

enum class ExpensesRetryTarget { Expenses, Categories, Both }
data class ExpensesLoadNotice(val target: ExpensesRetryTarget, val message: String, val retrying: Boolean)

fun expensesLoadNotices(state: DespesasUiState): List<ExpensesLoadNotice> = loadNotices(listOf(
    LoadFailure(ExpensesRetryTarget.Expenses, "Despesas", state.errorMessage, state.expensesNetworkFailure,
        state.expensesData.isNotEmpty() || state.effectiveExpensesData.isNotEmpty(), state.isLoading),
    LoadFailure(ExpensesRetryTarget.Categories, "Categorias", state.categoriesError, state.categoriesNetworkFailure,
        state.categoriesMap.isNotEmpty(), state.isCategoriesLoading)
)).map { notice ->
    ExpensesLoadNotice(if (notice.sources.size > 1) ExpensesRetryTarget.Both else notice.sources.single(),
        notice.message, notice.retrying)
}
