package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.paymentSources
import java.text.NumberFormat
import java.util.Locale

fun matchesExpenseType(expense: Expense, filter: String): Boolean {
    return when (filter) {
        "Parceladas" -> expense.type.equals("Parcelada", ignoreCase = true)
        "Únicas" -> expense.type.equals("Única", ignoreCase = true) ||
            expense.type.equals("Unica", ignoreCase = true)
        "Fixas" -> expense.type.equals("Fixa", ignoreCase = true)
        else -> true
    }
}

fun filterExpenses(
    expenses: List<Expense>,
    searchQuery: String,
    selectedFilter: String,
    categoryId: Int? = null,
    paymentSource: String? = null,
    paymentStatus: String? = null
): List<Expense> {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    val trimmedQuery = searchQuery.trim()
    val normalizedQuery = normalizeAmountSearchText(trimmedQuery)

    return expenses.filter { expense ->
        val normalizedCurrencyAmount = normalizeAmountSearchText(
            currencyFormatter.format(expense.amount)
        )
        val normalizedRawAmount = normalizeAmountSearchText(expense.amount.toString())
        val matchesSearch = trimmedQuery.isBlank() ||
            expense.description.contains(trimmedQuery, ignoreCase = true) ||
            (
                normalizedQuery.isNotBlank() &&
                    (
                        normalizedCurrencyAmount.contains(normalizedQuery) ||
                            normalizedRawAmount.contains(normalizedQuery)
                        )
                )

        matchesSearch && matchesExpenseType(expense, selectedFilter) &&
            (categoryId == null || expense.category_id == categoryId) &&
            (paymentSource == null || expense.paymentSources().any {
                it.equals(paymentSource, ignoreCase = true)
            }) && when (paymentStatus) {
                "paid" -> expense.is_paid
                "pending" -> !expense.is_paid
                else -> true
            }
    }
}

fun expenseCountsByFilter(
    expenses: List<Expense>,
    searchQuery: String,
    filters: List<String>
): Map<String, Int> {
    val searchMatches = filterExpenses(expenses, searchQuery, "Todas")
    return filters.associateWith { filter ->
        searchMatches.count { expense -> matchesExpenseType(expense, filter) }
    }
}

fun totalExpenseAmount(expenses: List<Expense>): Double =
    expenses.sumOf { expense -> expense.amount }

fun filteredExpenseTotal(
    effectiveExpenses: List<Expense>,
    searchQuery: String,
    selectedFilter: String,
    categoryId: Int? = null,
    paymentSource: String? = null,
    paymentStatus: String? = null
): Double = totalExpenseAmount(
    filterExpenses(
        expenses = effectiveExpenses,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        categoryId = categoryId,
        paymentSource = paymentSource,
        paymentStatus = paymentStatus
    )
)

fun expenseCategoryOptions(
    categories: Map<Int, String>,
    expenses: List<Expense>
): Map<Int, String> {
    val usedCategoryIds = expenses.mapTo(mutableSetOf()) { it.category_id }
    return categories.filterKeys { it in usedCategoryIds }
}

private fun normalizeAmountSearchText(value: String): String {
    return value
        .lowercase(Locale.forLanguageTag("pt-BR"))
        .replace("r$", "")
        .filter { it.isDigit() || it == ',' || it == '.' }
        .replace(',', '.')
}
