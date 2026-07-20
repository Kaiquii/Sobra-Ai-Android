package com.example.appfinanceiro.feature.home

import com.example.appfinanceiro.MainDispatcherRule
import com.example.appfinanceiro.core.data.HomeDataSource
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadAllPublishesSummaryIncomeExpensesAndCategories() = runTest(
        mainDispatcherRule.testDispatcher
    ) {
        val viewModel = HomeViewModel(FakeHomeDataSource())

        viewModel.loadAll(token = "token", month = 7, year = 2026)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2_000.0, state.summaryData?.salario ?: 0.0, 0.001)
        assertEquals(1, state.incomesData.size)
        assertEquals(1, state.expensesData.size)
        assertEquals("Mercado", state.categoriesMap[1])
        assertFalse(state.isSummaryLoading)
        assertFalse(state.isIncomesLoading)
        assertFalse(state.isExpensesLoading)
    }

    private class FakeHomeDataSource : HomeDataSource {
        override suspend fun getSummary(token: String, month: Int, year: Int) = SummaryResponse(
            month = month,
            year = year,
            salario = 0.0,
            adiantamento = 0.0,
            renda_extra_amt = 0.0,
            restante_salario = 1_900.0,
            restante_adiantamento = 0.0,
            restante_renda_extra = 0.0,
            total_gasto_salario = 100.0,
            total_gasto_adiantamento = 0.0,
            total_gasto_renda_extra = 0.0,
            total_expense = 100.0,
            total_geral_disponivel = 1_900.0,
            total_income = 2_000.0
        )

        override suspend fun getIncomes(
            token: String,
            month: Int?,
            year: Int?
        ) = IncomesResponse(
            incomes = listOf(Income(1, "Salário", 2_000.0, 7, 2026)),
            total = 1
        )

        override suspend fun getCategories(token: String) = CategoriesResponse(
            categories = listOf(Category(1, name = "Mercado")),
            total = 1
        )

        override suspend fun getExpenses(token: String, month: Int, year: Int) = ExpensesResponse(
            expenses = listOf(
                Expense(
                    id = 1,
                    category_id = 1,
                    amount = 100.0,
                    description = "Compra",
                    date = "2026-07-20T00:00:00-03:00",
                    type = "Única",
                    installments = null,
                    current_installment = null,
                    payment_source = "Salário"
                )
            ),
            total = 1
        )
    }
}
