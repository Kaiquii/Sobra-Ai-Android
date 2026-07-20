package com.example.appfinanceiro.feature.home

import com.example.appfinanceiro.core.network.Income

data class IncomeTotals(
    val salary: Double,
    val advance: Double,
    val extraIncome: Double
)

fun calculateIncomeTotals(incomes: List<Income>): IncomeTotals {
    return IncomeTotals(
        salary = incomes
            .filter {
                it.source.equals("Salario", ignoreCase = true) ||
                    it.source.equals("Salário", ignoreCase = true)
            }
            .sumOf { it.amount },
        advance = incomes
            .filter { it.source.equals("Adiantamento", ignoreCase = true) }
            .sumOf { it.amount },
        extraIncome = incomes
            .filter { it.source.equals("Renda Extra", ignoreCase = true) }
            .sumOf { it.amount }
    )
}
