package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.PaymentSplit
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

    @Test
    fun totalsOnlyTheExpensesVisibleAfterFiltering() {
        val filtered = filterExpenses(expenses, "", "Parceladas")

        assertEquals(250.00, totalExpenseAmount(filtered), 0.001)
    }

    @Test
    fun combinesCategorySourceStatusSearchAndType() {
        val matching = expenses[1].copy(category_id = 7, is_paid = true)
        val candidates = listOf(matching, matching.copy(id = 4, category_id = 8),
            matching.copy(id = 5, is_paid = false),
            matching.copy(id = 6, payment_source = "Adiantamento"),
            matching.copy(id = 7, type = "Fixa"))
        val result = filterExpenses(candidates, "note", "Parceladas", 7, "Salário", "paid")
        assertEquals(listOf(2), result.map { it.id })
        assertEquals(250.0, totalExpenseAmount(result), 0.001)
    }

    @Test
    fun paymentSourceIncludesSplitAndUsesSplitsInsteadOfLegacySource() {
        val split = expenses[0].copy(payment_source = "Salário", payment_splits = listOf(
            PaymentSplit(payment_source = "Adiantamento", amount = 100.0),
            PaymentSplit(payment_source = "Renda Extra", amount = 52.90)
        ))
        assertEquals(listOf(split), filterExpenses(listOf(split), "", "Todas",
            paymentSource = "Renda extra"))
        assertEquals(emptyList<Expense>(), filterExpenses(listOf(split), "", "Todas",
            paymentSource = "Salário"))
    }

    @Test
    fun clearingFiltersRestoresAllAndPendingExcludesPaid() {
        val candidates = listOf(expenses[0].copy(is_paid = true), expenses[1])
        assertEquals(listOf(2), filterExpenses(candidates, "", "Todas",
            paymentStatus = "pending").map { it.id })
        assertEquals(candidates, filterExpenses(candidates, "", "Todas"))
        assertEquals(emptyList<Expense>(), filterExpenses(candidates, "", "Todas", categoryId = 999))
    }

    @Test
    fun categoryOptionsOnlyContainRegisteredCategoriesWithExpenses() {
        val categories = mapOf(1 to "Mercado", 2 to "Casa", 3 to "Lazer")
        val scheduled = listOf(expenses[0], expenses[0].copy(id = 4),
            expenses[1].copy(category_id = 99))
        val incoming = listOf(expenses[2].copy(category_id = 2, isAdvanced = true))
        assertEquals(mapOf(1 to "Mercado", 2 to "Casa"),
            expenseCategoryOptions(categories, scheduled + incoming))
        assertEquals(emptyMap<Int, String>(), expenseCategoryOptions(categories, emptyList()))
    }

    @Test
    fun categoryOptionsFollowTheLoadedMonthWithoutChangingRegisteredCategories() {
        val categories = mapOf(1 to "Mercado", 2 to "Casa")
        assertEquals(mapOf(1 to "Mercado"), expenseCategoryOptions(categories, expenses))
        assertEquals(mapOf(2 to "Casa"), expenseCategoryOptions(categories,
            listOf(expenses[0].copy(category_id = 2))))
        assertEquals(2, categories.size)
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
