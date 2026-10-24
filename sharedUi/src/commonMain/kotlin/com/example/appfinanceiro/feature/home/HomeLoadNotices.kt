package com.example.appfinanceiro.feature.home

import com.example.appfinanceiro.core.designsystem.components.*

enum class HomeLoadSource { Summary, Incomes, Expenses, Categories }
fun homeLoadNotices(state: HomeUiState): List<LoadNotice<HomeLoadSource>> = loadNotices(listOf(
    LoadFailure(HomeLoadSource.Summary, "Resumo", state.summaryError, state.summaryNetworkFailure,
        state.summaryData != null, state.isSummaryLoading),
    LoadFailure(HomeLoadSource.Incomes, "Rendas", state.incomesError, state.incomesNetworkFailure,
        state.incomesData.isNotEmpty(), state.isIncomesLoading),
    LoadFailure(HomeLoadSource.Expenses, "Despesas", state.expensesError, state.expensesNetworkFailure,
        state.expensesData.isNotEmpty(), state.isExpensesLoading),
    LoadFailure(HomeLoadSource.Categories, "Categorias", state.categoriesError, state.categoriesNetworkFailure,
        state.categoriesMap.isNotEmpty(), state.isCategoriesLoading)
))
