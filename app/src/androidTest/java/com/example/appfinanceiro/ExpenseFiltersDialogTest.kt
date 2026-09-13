package com.example.appfinanceiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.test.espresso.Espresso
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.appfinanceiro.feature.despesas.components.ExpenseFiltersDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpenseFiltersDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changesAndClearAreDraftUntilApply() {
        var result: Triple<Int?, String?, String?>? = null
        compose.setContent {
            MaterialTheme {
                ExpenseFiltersDialog(mapOf(1 to "Mercado"), 1, "Salário", "paid", {},
                    { category, source, status -> result = Triple(category, source, status) })
            }
        }
        compose.onNodeWithContentDescription("Categoria: Mercado").assertExists()
        compose.onNodeWithText("Limpar filtros").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(null, result) }
        compose.onNodeWithContentDescription("Categoria: Todas").assertExists()
        compose.onNodeWithText("Aplicar filtros").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(Triple<Int?, String?, String?>(null, null, null), result) }
    }

    @Test fun closeDiscardsDraftAndReopeningUsesAppliedValues() {
        var open by mutableStateOf(true)
        var applied = false
        compose.setContent {
            MaterialTheme {
                if (open) ExpenseFiltersDialog(mapOf(1 to "Mercado"), 1, "Salário", "paid",
                    { open = false }, { _, _, _ -> applied = true })
            }
        }
        compose.onNodeWithText("Limpar filtros").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Fechar filtros").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, applied); open = true }
        compose.onNodeWithContentDescription("Categoria: Mercado").assertExists()
        compose.onNodeWithContentDescription("Status: Pagas").assertExists()
    }

    @Test fun selectStatusInDarkThemeOnlyAppliesOnConfirmation() {
        var result: String? = null
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ExpenseFiltersDialog(emptyMap(), null, null, null, {},
                    { _, _, status -> result = status })
            }
        }
        compose.onNodeWithContentDescription("Status: Todas").performScrollTo().performClick()
        compose.onNodeWithText("Pagas").performClick()
        compose.runOnIdle { assertEquals(null, result) }
        compose.onNodeWithText("Aplicar filtros").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("paid", result) }
    }

    @Test fun backDiscardsDraftWithoutApplying() {
        var open by mutableStateOf(true)
        var applied = false
        compose.setContent {
            MaterialTheme {
                if (open) ExpenseFiltersDialog(emptyMap(), null, "Salário", "paid",
                    { open = false }, { _, _, _ -> applied = true })
            }
        }
        compose.onNodeWithText("Limpar filtros").performScrollTo().performClick()
        Espresso.pressBack()
        compose.runOnIdle { assertEquals(false, open); assertEquals(false, applied) }
    }

}
