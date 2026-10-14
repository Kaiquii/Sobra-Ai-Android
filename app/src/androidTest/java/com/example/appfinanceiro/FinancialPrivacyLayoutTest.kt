package com.example.appfinanceiro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyButton
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyProvider
import com.example.appfinanceiro.core.designsystem.components.FinancialValue
import com.example.appfinanceiro.core.designsystem.components.PrivateReportContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FinancialPrivacyLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun hidingAndRevealingAnAmountKeepsItsBoundsAndHidesItsText() {
        compose.setContent {
            MaterialTheme {
                FinancialPrivacyProvider {
                    Column {
                        FinancialPrivacyButton()
                        Box(Modifier.testTag("amount")) {
                            FinancialValue("R$ 1.234,56", fontSize = 26.sp)
                        }
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("amount").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Ocultar valores").performClick()
        assertEquals(before, compose.onNodeWithTag("amount").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("R$ 1.234,56").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar valores").performClick()
        assertEquals(before, compose.onNodeWithTag("amount").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun reportSkeletonCompactsTheCardAndHidesDescendants() {
        compose.setContent {
            MaterialTheme {
                FinancialPrivacyProvider {
                    Column(Modifier.width(300.dp)) {
                        FinancialPrivacyButton()
                        Box(Modifier.testTag("report")) {
                            PrivateReportContent("Resumo") {
                                Column {
                                    Text("R$ 9.876,54")
                                    Box(Modifier.height(280.dp).width(200.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("report").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Ocultar valores").performClick()
        val hidden = compose.onNodeWithTag("report").fetchSemanticsNode().boundsInRoot
        assertEquals(before.width, hidden.width, 0f)
        assertTrue(hidden.height < before.height)
        compose.onNodeWithText("R$ 9.876,54").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar valores").performClick()
        assertEquals(before, compose.onNodeWithTag("report").fetchSemanticsNode().boundsInRoot)
    }
}
