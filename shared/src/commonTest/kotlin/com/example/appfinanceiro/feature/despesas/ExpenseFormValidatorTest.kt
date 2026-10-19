package com.example.appfinanceiro.feature.despesas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpenseFormValidatorTest {
    @Test
    fun requiresAmountDescriptionAndCategoryIndividually() {
        assertEquals("Campos", validateExpenseForm("", "Compra", 1, "", "Campos"))
        assertEquals("Campos", validateExpenseForm("10", "", 1, "", "Campos"))
        assertEquals("Campos", validateExpenseForm("10", "Compra", null, "", "Campos"))
        assertEquals("Campos", validateExpenseForm("", "", null, "", "Campos"))
    }

    @Test
    fun limitsNotesToFiveHundredCharacters() {
        assertNull(validateExpenseForm("10", "Compra", 1, "a".repeat(500), "Campos"))
        assertEquals(
            "Observações devem ter no máximo 500 caracteres.",
            validateExpenseForm("10", "Compra", 1, "a".repeat(501), "Campos")
        )
    }

    @Test
    fun requiredFieldsTakePrecedenceOverNotesLimit() {
        assertEquals("Campos", validateExpenseForm("", "Compra", 1, "a".repeat(501), "Campos"))
    }

    @Test
    fun acceptsTheCurrentValidFormRules() {
        assertNull(validateExpenseForm("10,50", "Compra", 1, "Observação", "Campos"))
        assertNull(validateExpenseForm("10", "Compra", 1, "", "Campos"))
        // A migração preserva a validação atual, inclusive a ausência de trim/parse.
        assertNull(validateExpenseForm(" ", " ", 1, "", "Campos"))
    }
}
