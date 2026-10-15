package com.example.appfinanceiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.appfinanceiro.core.designsystem.components.ExpenseDetailsDialog
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyProvider
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.createFinanceGson
import com.example.appfinanceiro.feature.despesas.DespesaListItem
import org.junit.Rule
import org.junit.Test

class ExpenseLegacyRenderingTest {
    @get:Rule val compose = createComposeRule()

    private fun legacyExpense(): Expense = createFinanceGson().fromJson(
        """{"id":10,"category_id":0,"amount":200,"description":null,
            "date":null,"type":null,"payment_splits":null}""",
        Expense::class.java
    )

    @Test
    fun cardRendersLegacyExpenseWithMissingDescriptionAndPaymentSplits() {
        val expense = legacyExpense()
        compose.setContent {
            MaterialTheme {
                FinancialPrivacyProvider {
                    DespesaListItem(expense, emptyMap(), {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("Despesa sem descrição").assertIsDisplayed()
        compose.onNodeWithText("Não informado").assertIsDisplayed()
    }

    @Test
    fun detailsRenderLegacyExpenseWithoutNullTextOrSplitFailures() {
        val expense = legacyExpense()
        compose.setContent {
            MaterialTheme {
                FinancialPrivacyProvider {
                    ExpenseDetailsDialog(expense, "Sem categoria", onDismiss = {})
                }
            }
        }
        compose.onNodeWithText("Despesa sem descrição").assertIsDisplayed()
        compose.onNodeWithText("Sem categoria").assertIsDisplayed()
        compose.onNodeWithText("Fechar").assertIsDisplayed()
    }
}
