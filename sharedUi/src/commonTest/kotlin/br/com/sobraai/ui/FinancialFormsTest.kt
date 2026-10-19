package br.com.sobraai.ui

import com.example.appfinanceiro.core.network.*
import kotlin.test.*

class FinancialFormsTest {
    private fun valid() = ExpenseDraft("100,00", "Mercado", " notas ", 1, date = "20/10/2026")

    @Test fun rejectsInvalidNumbersWithoutSilentlyConvertingThemToZero() {
        listOf("", "0", "-1", "1e4", "NaN", "Infinity", "1.234,56", "1,2,3").forEach { assertNull(parseAmount(it), it) }
        assertEquals(1234.56, parseAmount("1234,56"))
        assertEquals(.01, parseAmount("0.01"))
        assertNull(valid().copy(amount = "0").validate())
        assertEquals(0.0, valid().copy(amount = "0").createRequest().amount)
        assertEquals(10.001, parseAmount("10,001"))
        assertEquals(.00001, parseAmount(amountInput(.00001)))
        assertNotNull(valid().copy(notes = "a".repeat(501)).validate())
        assertNotNull(valid().copy(description = "   ").validate())
        assertNotNull(valid().copy(categoryId = null).validate())
    }
    @Test fun installmentsAndSplitsPreserveTheExpenseTotalAndWireValues() {
        val draft = valid().copy(type = "Parcelada", installments = "6", split = true,
            splits = listOf(SplitDraft("Salário", "70,10"), SplitDraft("Adiantamento", "29,90")))
        assertNull(draft.validate())
        val request = draft.createRequest()
        assertEquals(100.0, request.amount)
        assertEquals(6, request.installments)
        assertNull(request.payment_source)
        assertEquals(listOf("Salário", "Adiantamento"), request.payment_splits!!.map { it.payment_source })
        assertEquals(100.0, request.payment_splits!!.sumOf { it.amount })
        assertEquals("notas", request.notes)
        assertTrue(request.date.startsWith("2026-10-20T00:00:00"))
        assertNotNull(draft.copy(installments = "0").validate())
        assertNotNull(draft.copy(splits = draft.splits + SplitDraft("Salario", "1")).validate())
        assertNotNull(draft.copy(splits = listOf(SplitDraft("Salário", "99,99"))).validate())
        assertNotNull(draft.copy(splits = listOf(SplitDraft("Salário", "0"), SplitDraft("Renda Extra", "100"))).validate())
    }
    @Test fun editingDoesNotChangeTypeInstallmentsStatusOrAdvanceIncidentally() {
        val original = testExpense(type = "Parcelada").copy(payment_splits = listOf(PaymentSplit(payment_source = "Salario", amount = 100.0)),
            payment_source = null, notes = "anotação")
        val draft = ExpenseDraft.from(original)
        assertFalse(draft.split)
        assertEquals("Salario", draft.source)
        val request = draft.copy(amount = "200,50", updateFuture = true).updateRequest(original)
        assertNull(request.type)
        assertEquals(true, request.update_future)
        assertEquals(200.50, request.amount)
        assertEquals("anotação", request.notes)
        assertNull(valid().updateRequest(testExpense(type = "Única")).update_future)
        assertEquals("", ExpenseDraft.from(original.copy(date = "legado inválido")).date)
        assertNull(valid().updateRequest(original.copy(type = "")).type)
        val negativeLegacy = original.copy(amount = -.00001, payment_splits = emptyList(), payment_source = "Salário")
        assertEquals(negativeLegacy.amount, ExpenseDraft.from(negativeLegacy).updateRequest(negativeLegacy).amount)
    }
    @Test fun calendarHandlesLeapYearsAndUtcPickerWithoutChangingTheDay() {
        assertNull(FinancialDate.parseDisplay("29/02/2025"))
        assertNull(FinancialDate.parseDisplay("31/04/2026"))
        assertNull(FinancialDate.parseApi("2026-13-01"))
        val dates = listOf(FinancialDate(1970, 1, 1), FinancialDate(1969, 12, 31), FinancialDate(2000, 2, 29), FinancialDate(2026, 12, 31), FinancialDate(2027, 1, 1))
        dates.forEach { assertEquals(it, FinancialDate.fromUtcMillis(it.utcMillis)); assertEquals(it, FinancialDate.parseDisplay(it.display)) }
        assertEquals(0L, FinancialDate(1970, 1, 1).utcMillis)
        assertEquals(FinancialDate(2026, 10, 20), FinancialDate.parseApi("2026-10-20T23:00:00-12:00"))
        assertTrue(FinancialDate(2026, 10, 19) < FinancialDate(2026, 10, 20))
    }
    @Test fun incomePayloadsKeepTheFutureFlagsAndSalarySpelling() {
        val salary = IncomeDraft("Salario", Period(10, 2026), amount = "1500,50").createRequest()
        assertEquals("Salario", salary.source)
        assertEquals("Fixa", salary.type)
        assertNull(salary.repeat_future)
        assertEquals(false, IncomeDraft("Renda Extra", Period(10, 2026), amount = "100").createRequest().repeat_future)
        assertEquals(true, IncomeDraft("Renda Extra", Period(10, 2026), amount = "100", repeatFuture = true).createRequest().repeat_future)
    }
}
