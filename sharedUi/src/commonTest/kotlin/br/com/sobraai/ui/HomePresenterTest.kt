package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

private fun summary(month: Int, year: Int) = SummaryResponse(month, year, 0.0, 0.0, 0.0, 800.0, 50.0, 0.0, 200.0, 0.0, 0.0, 200.0, 850.0, 1050.0)
private fun expense(id: Int, paid: Boolean, splits: List<PaymentSplit> = emptyList()) = Expense(id, id, 100.0, "Teste", "2026-10-01", "Fixa", null, null, "Salario", splits, is_paid = paid)

private class HomeFake : HomeDataSource {
    var fail = false
    var expired = false
    var delayMonth: Int? = null
    val pending = CompletableDeferred<Unit>()
    var effectiveCalls = 0
    override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        if (month == delayMonth) withContext(NonCancellable) { pending.await() }
        if (expired) throw SessionExpiredException()
        if (fail) error("offline")
        return summary(month, year)
    }
    override suspend fun getIncomes(token: String, month: Int?, year: Int?) = IncomesResponse(listOf(
        Income(1, "Salário", 1000.0, month!!, year!!), Income(2, "Salario", 200.0, month, year),
        Income(3, "Adiantamento", 50.0, month, year)), 3)
    override suspend fun getCategories(token: String) = CategoriesResponse(listOf(Category(1, name = "Casa"), Category(2, name = "Outros")), 2)
    override suspend fun getExpenses(token: String, month: Int, year: Int, paymentStatus: String?): ExpensesResponse = error("Use effective period")
    override suspend fun getEffectiveExpenses(token: String, month: Int, year: Int, paymentStatus: String?): ExpensesResponse {
        effectiveCalls++
        assertNull(paymentStatus)
        return ExpensesResponse(listOf(expense(1, true, listOf(PaymentSplit(amount = 20.0, payment_source = "Renda Extra"))), expense(2, false)), 2)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterTest {
    @Test fun loadsAtomicEffectivePeriodAndIncomeAliasesWithoutChangingBalances() = runTest {
        val fake = HomeFake()
        val presenter = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        presenter.refresh(); runCurrent()
        val data = assertNotNull(presenter.state.value.snapshot)
        assertEquals(1200.0, data.summary.salario)
        assertEquals(800.0, data.summary.restante_salario)
        assertEquals(850.0, data.summary.total_geral_disponivel)
        assertEquals(1, fake.effectiveCalls)
        presenter.filter(null, "Renda Extra", "paid")
        assertEquals(listOf(1), presenter.state.value.filteredExpenses.map { it.id })
        presenter.filter(2, "Salário", "pending")
        assertEquals(listOf(2), presenter.state.value.filteredExpenses.map { it.id })
    }

    @Test fun outOfOrderMonthCannotReplaceNewPeriod() = runTest {
        val fake = HomeFake().apply { delayMonth = 10 }
        val presenter = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        presenter.refresh(); runCurrent()
        presenter.selectPeriod(Period(11, 2026)); runCurrent()
        assertEquals(Period(11, 2026), presenter.state.value.snapshot?.period)
        fake.pending.complete(Unit); runCurrent()
        assertEquals(Period(11, 2026), presenter.state.value.snapshot?.period)
        assertFalse(presenter.state.value.loading)
    }

    @Test fun failedNewMonthRetainsLabeledSnapshotAndRetryKeepsErrorUntilSuccess() = runTest {
        val fake = HomeFake()
        val presenter = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        presenter.refresh(); runCurrent()
        fake.fail = true; presenter.selectPeriod(Period(11, 2026)); runCurrent()
        assertEquals(Period(10, 2026), presenter.state.value.snapshot?.period)
        assertEquals(Period(11, 2026), presenter.state.value.period)
        assertNotNull(presenter.state.value.error)
        fake.fail = false; presenter.refresh()
        assertNotNull(presenter.state.value.error)
        runCurrent()
        assertNull(presenter.state.value.error)
        assertEquals(Period(11, 2026), presenter.state.value.snapshot?.period)
    }

    @Test fun expiredSessionClearsAllFinancialData() = runTest {
        val fake = HomeFake()
        var expired = 0
        val presenter = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { expired++ }
        presenter.refresh(); runCurrent()
        fake.expired = true; presenter.refresh(); runCurrent()
        assertNull(presenter.state.value.snapshot)
        assertEquals(1, expired)
    }

    @Test fun currencyAndCalendarRemainBrazilian() {
        assertEquals("R$ 1.234,56", formatCurrency(1234.56))
        assertEquals("-R$ 0,01", formatCurrency(-0.01))
        assertEquals("R$ 1,00", formatCurrency(.999))
        assertEquals("01/10/2026", formatDate("2026-10-01T12:00:00Z"))
        assertEquals(Period(1, 2027), Period(12, 2026).shift(1))
        assertEquals(Period(12, 2025), Period(1, 2026).shift(-1))
    }

    @Test fun unexpectedBackendPeriodsCannotRelabelOtherMonthsIncomeOrSummary() = runTest {
        val fake = EditingFake()
        var wrongSummary = false
        val source = object : HomeDataSource by fake {
            override suspend fun getIncomes(token: String, month: Int?, year: Int?) = IncomesResponse(listOf(
                Income(1, "Salario", 1000.0, 10, 2026), Income(2, "Salário", 9000.0, 11, 2026)), 2)
            override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse =
                fake.getSummary(token, if (wrongSummary) 10 else month, year)
        }
        val presenter = HomePresenter(source, "synthetic-token", backgroundScope, Period(10, 2026)) {}
        presenter.refresh(); runCurrent()
        assertEquals(1000.0, presenter.state.value.snapshot?.summary?.salario)
        assertEquals(1, presenter.state.value.snapshot?.incomes?.size)
        wrongSummary = true
        presenter.selectPeriod(Period(11, 2026)); runCurrent()
        assertEquals(Period(10, 2026), presenter.state.value.snapshot?.period)
        assertEquals(Period(11, 2026), presenter.state.value.period)
        assertNotNull(presenter.state.value.error)
    }
}
