package com.example.appfinanceiro.feature.home

import com.example.appfinanceiro.core.network.Income
import org.junit.Assert.assertEquals
import org.junit.Test

class IncomeCalculationsTest {
    @Test
    fun groupsAndSumsAllIncomeSources() {
        val result = calculateIncomeTotals(
            listOf(
                income(1, "Salário", 1_000.0),
                income(2, "Salario", 500.0),
                income(3, "Adiantamento", 300.0),
                income(4, "Renda Extra", 125.5)
            )
        )

        assertEquals(1_500.0, result.salary, 0.001)
        assertEquals(300.0, result.advance, 0.001)
        assertEquals(125.5, result.extraIncome, 0.001)
    }

    private fun income(id: Int, source: String, amount: Double) = Income(
        id = id,
        source = source,
        amount = amount,
        month = 7,
        year = 2026
    )
}
