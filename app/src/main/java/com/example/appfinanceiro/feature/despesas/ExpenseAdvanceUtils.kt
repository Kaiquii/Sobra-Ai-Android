package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun incomingAdvancedExpenses(
    effectiveExpenses: List<Expense>,
    selectedMonth: Int,
    selectedYear: Int
): List<Expense> = effectiveExpenses
    .filter { expense ->
        expense.isAdvanced && expense.scheduledMonthYear()?.let { (month, year) ->
            month != selectedMonth || year != selectedYear
        } == true
    }
    .distinctBy { it.id }

fun Expense.scheduledMonthYear(): Pair<Int, Int>? {
    val datePart = date.orEmpty().substringBefore('T').substringBefore(' ')
    val parts = datePart.split('-')
    if (parts.size == 3) {
        val year = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        if (year != null && month in 1..12) return month!! to year
    }

    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).parse(date)
            ?: return@runCatching null
        Calendar.getInstance().apply { time = parsed }.let {
            (it.get(Calendar.MONTH) + 1) to it.get(Calendar.YEAR)
        }
    }.getOrNull()
}

fun monthName(month: Int): String = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
).getOrElse(month - 1) { "outro mês" }

fun formatAdvancedDate(value: String?): String? {
    val parts = value?.substringBefore('T')?.substringBefore(' ')?.split('-') ?: return null
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else null
}
