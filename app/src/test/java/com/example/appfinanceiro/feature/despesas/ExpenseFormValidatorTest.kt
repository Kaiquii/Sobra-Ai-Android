package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.feature.despesas.components.PaymentSplitInput
import com.example.appfinanceiro.feature.despesas.components.paymentSplitValidationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseFormValidatorTest {
    @Test
    fun acceptsPaymentSplitsThatMatchTheExpenseAmount() {
        val result = paymentSplitValidationMessage(
            amount = 1200.0,
            splits = listOf(
                PaymentSplitInput("Salário", "1000,00"),
                PaymentSplitInput("Renda Extra", "200,00")
            )
        )

        assertNull(result)
    }

    @Test
    fun rejectsRepeatedOrIncompletePaymentSplits() {
        val repeated = paymentSplitValidationMessage(
            amount = 1200.0,
            splits = listOf(
                PaymentSplitInput("Salário", "600"),
                PaymentSplitInput("Salário", "600")
            )
        )
        val incomplete = paymentSplitValidationMessage(
            amount = 1200.0,
            splits = listOf(PaymentSplitInput("Salário", "1000"))
        )

        assertEquals("Não repita uma origem de pagamento.", repeated)
        assertEquals("Distribua todo o valor da despesa entre as origens.", incomplete)
    }
}
