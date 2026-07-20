package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.Expense
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
    selectedFilter: String
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

        matchesSearch && matchesExpenseType(expense, selectedFilter)
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

private fun normalizeAmountSearchText(value: String): String {
    return value
        .lowercase(Locale.forLanguageTag("pt-BR"))
        .replace("r$", "")
        .filter { it.isDigit() || it == ',' || it == '.' }
        .replace(',', '.')
}
