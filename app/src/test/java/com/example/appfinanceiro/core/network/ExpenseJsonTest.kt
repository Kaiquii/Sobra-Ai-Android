package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.feature.despesas.expenseCountsByFilter
import com.example.appfinanceiro.feature.despesas.filterExpenses
import com.example.appfinanceiro.feature.despesas.scheduledMonthYear
import com.example.appfinanceiro.feature.despesas.totalExpenseAmount
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class ExpenseJsonTest {
    private val gson = createFinanceGson()

    private fun read(fields: String): Expense = gson.fromJson(
        """{"id":10,"amount":200.0,$fields}""", Expense::class.java
    )

    @Test
    fun missingAndNullDescriptionsRemainSearchableByAmountWithoutInventingDescription() {
        for (fields in listOf("\"category_id\":0", "\"description\":null", "\"description\":\"\"")) {
            val expense = read(fields)
            assertEquals("", expense.description)
            assertEquals("Despesa sem descrição", expense.displayDescription)
            assertEquals(listOf(expense), filterExpenses(listOf(expense), "", "Todas"))
            assertEquals(listOf(expense), filterExpenses(listOf(expense), "200,00", "Todas"))
            assertTrue(filterExpenses(listOf(expense), "compra", "Todas").isEmpty())
            assertEquals(1, expenseCountsByFilter(listOf(expense), "200", listOf("Todas"))["Todas"])
            assertEquals("", JsonParser.parseString(gson.toJson(expense)).asJsonObject["description"].asString)
        }
    }

    @Test
    fun nullablePresentationFieldsAndSplitsAreSafeForCardsAndAdvanceDates() {
        for (fields in listOf("\"description\":\"Compra\"", "\"date\":null,\"type\":null,\"payment_splits\":null")) {
            val expense = read(fields)
            assertEquals("", expense.date)
            assertEquals("", expense.type)
            assertTrue(expense.payment_splits.isEmpty())
            assertEquals("Não informado", expense.paymentCardSourceLabel())
            assertNull(expense.scheduledMonthYear())
        }
        val expense = read(""""payment_splits":[null,{"payment_source":null,"amount":50.0}],"payment_source":"Salário"""")
        assertEquals(1, expense.payment_splits.size)
        assertEquals(50.0, expense.payment_splits.single().amount, 0.0)
        assertEquals("", expense.payment_splits.single().payment_source)
        assertEquals(listOf("Salário"), expense.paymentSources())
    }

    @Test
    fun searchAndSourceHelpersAlsoTolerateObjectsFromUnconfiguredGson() {
        val expense = Gson().fromJson("""{"id":10,"amount":200,"description":null}""", Expense::class.java)
        assertEquals(listOf(expense), filterExpenses(listOf(expense), "200", "Todas"))
        assertEquals("Despesa sem descrição", expense.displayDescription)
        assertEquals("Não informado", expense.paymentCardSourceLabel())
        assertNull(expense.scheduledMonthYear())
    }

    @Test
    fun nestedExpenseResponsesPreserveFinancialFieldsAndInstallmentAmounts() {
        val json = """{"expenses":[{"id":10,"category_id":4,"amount":200,"description":"Compra",
            "date":"2026-10-15T00:00:00-03:00","type":"Parcelada","installments":3,
            "current_installment":1,"is_paid":true,"is_advanced":true,
            "payment_splits":[{"payment_source":"Salário","amount":200}]}],"total":1}"""
        val response = gson.fromJson(json, ExpensesResponse::class.java)
        val expense = response.expenses.single()
        assertEquals(10, expense.id)
        assertEquals(4, expense.category_id)
        assertEquals("Compra", expense.displayDescription)
        assertEquals("Parcelada", expense.type)
        assertEquals(3, expense.installments)
        assertEquals(1, expense.current_installment)
        assertTrue(expense.is_paid)
        assertTrue(expense.isAdvanced)
        assertEquals(10 to 2026, expense.scheduledMonthYear())
        assertEquals(listOf("Salário"), expense.paymentSources())
        val installments = (1..3).map { expense.copy(id = it, current_installment = it) }
        assertEquals(600.0, totalExpenseAmount(installments), 0.0)
        assertEquals(3, filterExpenses(installments, "200", "Parceladas").size)
        assertTrue(filterExpenses(installments, "600", "Todas").isEmpty())
        assertEquals(expense, gson.fromJson(gson.toJson(expense), Expense::class.java))
    }
}
