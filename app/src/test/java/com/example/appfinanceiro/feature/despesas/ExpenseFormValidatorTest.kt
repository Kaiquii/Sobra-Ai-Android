package com.example.appfinanceiro.feature.despesas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseFormValidatorTest {
    @Test
    fun requiresAmountDescriptionAndCategory() {
        val result = validateExpenseForm("", "", null, "", "Campos obrigatórios")
        assertEquals("Campos obrigatórios", result)
    }

    @Test
    fun limitsNotesToFiveHundredCharacters() {
        val result = validateExpenseForm("10", "Compra", 1, "a".repeat(501), "Campos")
        assertEquals("Observações devem ter no máximo 500 caracteres.", result)
    }

    @Test
    fun acceptsTheCurrentValidFormRules() {
        val result = validateExpenseForm("10,50", "Compra", 1, "Observação", "Campos")
        assertNull(result)
    }
}
