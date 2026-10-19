package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.network.PaymentSplit
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesPresenterTest {
    @Test fun effectiveTotalIncludesIncomingAdvancesWithoutCountingOutgoingTwice() = runTest {
        val outgoing = testExpense(1, advanced = true, advancedAt = "2026-09-01", amount = 100.0)
        val normal = testExpense(2, amount = 50.0)
        val incoming = testExpense(3, date = "2026-11-20T00:00:00-03:00", advanced = true, advancedAt = "2026-10-10", amount = 25.0)
        val fake = EditingFake().apply { scheduled = listOf(outgoing, normal); effective = listOf(normal, incoming, incoming) }
        val presenter = ExpensesPresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        presenter.refresh(); runCurrent()
        assertEquals(listOf(1, 2), presenter.state.value.scheduled.map { it.id })
        assertEquals(listOf(3), presenter.state.value.incoming.map { it.id })
        assertEquals(75.0, presenter.state.value.total)
        assertEquals(1, fake.scheduledReads)
        assertEquals(1, fake.effectiveReads)
    }
    @Test fun typeDescriptionAmountSplitAndStatusFiltersCombineWithoutChangingAmounts() {
        val split = testExpense(1, "Parcelada", amount = 1234.56).copy(description = "Compra", is_paid = true,
            payment_splits = listOf(PaymentSplit(payment_source = "Salario", amount = 1000.0), PaymentSplit(payment_source = "Renda Extra", amount = 234.56)))
        val ordinary = testExpense(2, amount = 50.0)
        assertEquals(listOf(split), ExpenseFilters("1.234,56", "Parceladas", 1, "Salário", "paid").apply(listOf(split, ordinary)))
        assertEquals(listOf(split), ExpenseFilters("comp", "Todas", source = "Renda Extra").apply(listOf(split, ordinary)))
        assertEquals(listOf(ordinary), ExpenseFilters(status = "pending").apply(listOf(split, ordinary)))
        assertEquals(1234.56, split.amount)
    }
    @Test fun staleMonthAndFailedRetryRetainAnExplicitlyLabeledSnapshot() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fake = EditingFake()
        val presenter = ExpensesPresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        presenter.refresh(); runCurrent()
        fake.readFailure = IllegalStateException("offline")
        presenter.selectPeriod(Period(11, 2026)); runCurrent()
        assertEquals(Period(10, 2026), presenter.state.value.snapshot?.period)
        assertEquals(Period(11, 2026), presenter.state.value.period)
        assertNotNull(presenter.state.value.error)
        fake.readFailure = null; fake.beforeRead = { month -> if (month == 11) withContext(NonCancellable) { gate.await() } }
        presenter.refresh(); runCurrent()
        assertNotNull(presenter.state.value.error)
        presenter.selectPeriod(Period(12, 2026)); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(Period(12, 2026), presenter.state.value.snapshot?.period)
        assertNull(presenter.state.value.error)
    }
    @Test fun categoryOptionsUseAllVisibleDataAndKeepASelectionAcrossPeriods() = runTest {
        val fake = EditingFake().apply {
            scheduled = listOf(testExpense(1).copy(category_id = 1))
            effective = listOf(testExpense(2, date = "2026-11-01", advanced = true).copy(category_id = 2))
        }
        val presenter = ExpensesPresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        presenter.refresh(); runCurrent()
        presenter.filter(ExpenseFilters(search = "absent", category = 2))
        assertEquals(setOf(1, 2), presenter.state.value.snapshot?.categoryOptions?.keys)
        presenter.selectPeriod(Period(11, 2026)); runCurrent()
        assertEquals(2, presenter.state.value.filters.category)
    }
    @Test fun expirationClearsSnapshotInsteadOfDisplayingAnotherAccountsData() = runTest {
        val fake = EditingFake()
        var expired = 0
        val presenter = ExpensesPresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { expired++ }
        presenter.refresh(); runCurrent()
        fake.readFailure = SessionExpiredException(); presenter.refresh(); runCurrent()
        assertEquals(1, expired)
        assertNull(presenter.state.value.snapshot)
        assertFalse(presenter.state.value.loading)
    }
}
