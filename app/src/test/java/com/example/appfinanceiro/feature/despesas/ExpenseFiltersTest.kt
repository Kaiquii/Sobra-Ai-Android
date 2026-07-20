package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.Expense
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseFiltersTest {
    private val expenses = listOf(
        expense(1, "Mercado", 152.90, "Única"),
        expense(2, "Notebook", 250.00, "Parcelada"),
        expense(3, "Aluguel", 1_200.00, "Fixa")
    )

    @Test
    fun filtersByDescriptionIgnoringCase() {
        val result = filterExpenses(expenses, "mercado", "Todas")
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun filtersByBrazilianCurrencyValue() {
        val result = filterExpenses(expenses, "152,90", "Todas")
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun combinesSearchAndExpenseType() {
        val result = filterExpenses(expenses, "250", "Parceladas")
        assertEquals(listOf(2), result.map { it.id })
    }

    @Test
    fun countsEveryFilterAfterSearch() {
        val result = expenseCountsByFilter(
            expenses = expenses,
            searchQuery = "",
            filters = listOf("Todas", "Parceladas", "Únicas", "Fixas")
        )

        assertEquals(3, result["Todas"])
        assertEquals(1, result["Parceladas"])
        assertEquals(1, result["Únicas"])
        assertEquals(1, result["Fixas"])
    }

    private fun expense(id: Int, description: String, amount: Double, type: String) = Expense(
        id = id,
        category_id = 1,
        amount = amount,
        description = description,
        date = "2026-07-20T00:00:00-03:00",
        type = type,
        installments = null,
        current_installment = null,
        payment_source = "Salário"
    )
}
