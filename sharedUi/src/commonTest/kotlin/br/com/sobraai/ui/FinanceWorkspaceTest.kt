package br.com.sobraai.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceWorkspaceTest {
    @Test fun successfulIncomeMutationReloadsBothScreensAndUsesCanonicalSalarySource() = runTest {
        val fake = EditingFake()
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) { error("unexpected") }
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) { error("unexpected") }
        home.refresh(); workspace.navigate(FinanceRoute.Expenses); runCurrent()
        val beforeHome = fake.summaryReads; val beforeExpenses = fake.scheduledReads
        workspace.editIncome("Salário", null, Period(10, 2026))
        workspace.income.value!!.update { it.copy(amount = "200") }
        workspace.income.value!!.save(); runCurrent()
        assertEquals("Salario", fake.createdIncome?.source)
        assertNull(fake.createdIncome?.repeat_future)
        assertNull(workspace.income.value)
        assertTrue(fake.summaryReads > beforeHome)
        assertTrue(fake.scheduledReads > beforeExpenses)
        workspace.close()
    }
    @Test fun deletingFixedAndInstallmentExpensesPreservesDifferentFutureContracts() = runTest {
        val fake = EditingFake()
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) {}
        workspace.deleteExpense(testExpense(1, "Fixa"), true, workspace.newAction()); runCurrent()
        assertEquals(1 to null, fake.deletedExpense)
        workspace.deleteExpense(testExpense(2, "Parcelada"), true, workspace.newAction()); runCurrent()
        assertEquals(2 to true, fake.deletedExpense)
        workspace.deleteExpense(testExpense(3, "Parcelada"), false, workspace.newAction()); runCurrent()
        assertEquals(3 to null, fake.deletedExpense)
        workspace.close()
    }
    @Test fun advanceMustBeBeforeScheduledDateAndNeverChangesPaymentStatus() = runTest {
        val fake = EditingFake()
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) {}
        val expense = testExpense(type = "Única", date = "2026-10-20T00:00:00-03:00")
        val invalid = workspace.newAction()
        workspace.advance(expense, FinancialDate(2026, 10, 20), invalid); runCurrent()
        assertEquals(0, fake.writes)
        assertNotNull(invalid.state.value.error)
        workspace.advance(expense, FinancialDate(2026, 9, 30), workspace.newAction()); runCurrent()
        assertEquals(Triple(1, true, "2026-09-30"), fake.advanced)
        assertNull(fake.paid)
        workspace.advance(expense, null, workspace.newAction()); runCurrent()
        assertEquals(Triple(1, false, null), fake.advanced)
        assertNull(fake.paid)
        workspace.setPaid(expense, true, workspace.newAction()); runCurrent()
        assertEquals(1 to true, fake.paid)
        workspace.close()
    }
    @Test fun closingWorkspaceCancelsLateWritesAndDoesNotReloadPreviousSession() = runTest {
        val fake = EditingFake()
        val gate = CompletableDeferred<Unit>()
        fake.beforeWrite = { withContext(NonCancellable) { gate.await() } }
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) {}
        val runner = workspace.newAction()
        workspace.setPaid(testExpense(), true, runner); runCurrent()
        workspace.close(); gate.complete(Unit); runCurrent()
        assertEquals(0, fake.summaryReads)
        assertFalse(runner.state.value.completed)
        assertNull(workspace.income.value)
        assertNull(workspace.expenseEditor)
    }
    @Test fun expenseSaveReturnsToTheListAndRefreshesBothPeriods() = runTest {
        val fake = EditingFake()
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) {}
        workspace.editExpense(1); runCurrent()
        workspace.expenseEditor!!.update { it.copy(amount = "225") }
        workspace.expenseEditor!!.save(); runCurrent()
        assertEquals(FinanceRoute.Expenses, workspace.route.value)
        assertNull(workspace.expenseEditor)
        assertEquals(225.0, fake.updatedExpense?.second?.amount)
        assertTrue(fake.summaryReads > 0)
        assertTrue(fake.scheduledReads > 0)
        workspace.close()
    }

    @Test fun reopeningDetailsDuringAWriteCannotStartAnotherMutation() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fake = EditingFake().apply { beforeWrite = { gate.await() } }
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(10, 2026)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) {}
        val first = workspace.newAction()
        workspace.setPaid(testExpense(), true, first); runCurrent()
        val reopened = workspace.newAction()
        assertSame(first, reopened)
        workspace.setPaid(testExpense(), true, reopened); runCurrent()
        assertEquals(1, fake.writes)
        gate.complete(Unit); runCurrent()
        workspace.close()
    }

    @Test fun newExpenseFromIncomeSettingsUsesTheMonthBeingViewed() = runTest {
        val fake = EditingFake()
        val home = HomePresenter(fake, "synthetic-token", backgroundScope, Period(12, 2027)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic-token", backgroundScope) {}
        workspace.navigate(FinanceRoute.Incomes)
        workspace.editExpense(); runCurrent()
        assertEquals("01/12/2027", workspace.expenseEditor?.state?.value?.draft?.date)
        workspace.close()
    }
}
