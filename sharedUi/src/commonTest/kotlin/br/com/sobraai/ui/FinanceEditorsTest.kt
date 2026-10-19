package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceEditorsTest {
    @Test fun createsCategoryAndSplitExpenseOnlyOnceAndNotifiesOnSuccess() = runTest {
        val fake = EditingFake()
        var saved = 0
        val editor = ExpenseEditor(fake, "synthetic-token", backgroundScope, null, { error("unexpected") }, { saved++ })
        editor.load(); runCurrent()
        editor.createCategory("  Transporte  ") {}; runCurrent()
        assertEquals(3, editor.state.value.draft.categoryId)
        assertEquals("Transporte", fake.categories.last().name)
        editor.update { it.copy(amount = "100", description = "Conta", date = "20/10/2026", type = "Parcelada", installments = "3",
            split = true, splits = listOf(SplitDraft("Salário", "60"), SplitDraft("Renda Extra", "40"))) }
        val gate = CompletableDeferred<Unit>()
        fake.beforeWrite = { gate.await() }
        editor.save(); editor.save(); runCurrent()
        assertEquals(2, fake.writes) // one category, one expense
        assertTrue(editor.mutation.state.value.busy)
        gate.complete(Unit); runCurrent()
        assertEquals(1, saved)
        assertEquals(100.0, fake.createdExpense?.amount)
        assertEquals(3, fake.createdExpense?.installments)
    }
    @Test fun failedWriteKeepsDraftAndRetryDoesNotReportFalseSuccess() = runTest {
        val fake = EditingFake()
        var saved = 0
        val editor = ExpenseEditor(fake, "synthetic-token", backgroundScope, 1, { error("unexpected") }, { saved++ })
        editor.load(); runCurrent()
        editor.update { it.copy(amount = "150", description = "Descrição corrigida") }
        fake.writeFailure = IllegalStateException("offline")
        editor.save(); runCurrent()
        assertEquals(0, saved)
        assertEquals("Descrição corrigida", editor.state.value.draft.description)
        assertFalse(editor.mutation.state.value.busy)
        assertNotNull(editor.mutation.state.value.error)
        fake.writeFailure = null; editor.save(); runCurrent()
        assertEquals(1, saved)
        assertEquals(150.0, fake.updatedExpense?.second?.amount)
    }
    @Test fun invalidFormAndUnconfirmedFutureCannotSendMutations() = runTest {
        val fake = EditingFake()
        val editor = ExpenseEditor(fake, "synthetic-token", backgroundScope, 1, { error("unexpected") }, {})
        editor.load(); runCurrent()
        editor.update { it.copy(amount = "zero") }; editor.save(); runCurrent()
        assertEquals(0, fake.writes)
        editor.update { it.copy(amount = "150", updateFuture = true) }; editor.save(); runCurrent()
        assertEquals(0, fake.writes)
        editor.save(true); runCurrent()
        assertEquals(true, fake.updatedExpense?.second?.update_future)
        assertNull(fake.updatedExpense?.second?.type)
    }
    @Test fun categoryFailureCanRetryAndDoesNotClearTheForm() = runTest {
        val fake = EditingFake()
        val editor = ExpenseEditor(fake, "synthetic-token", backgroundScope, null, { error("unexpected") }, {})
        editor.load(); runCurrent(); editor.update { it.copy(description = "Rascunho") }
        fake.writeFailure = IllegalStateException("offline")
        editor.createCategory("Viagens") {}; runCurrent()
        assertNotNull(editor.state.value.categoryError)
        assertFalse(editor.state.value.categoryBusy)
        assertEquals("Rascunho", editor.state.value.draft.description)
        fake.writeFailure = null; editor.createCategory("Viagens") {}; runCurrent()
        assertNull(editor.state.value.categoryError)
        assertEquals(3, editor.state.value.draft.categoryId)
    }
    @Test fun expenseLoadFailureCanRetryButCannotSaveAnUnloadedExpense() = runTest {
        val fake = EditingFake().apply { readFailure = IllegalStateException("offline") }
        val editor = ExpenseEditor(fake, "synthetic-token", backgroundScope, 1, { error("unexpected") }, {})
        editor.load(); runCurrent(); editor.save(); runCurrent()
        assertEquals(0, fake.writes)
        assertNotNull(editor.state.value.loadError)
        fake.readFailure = null; editor.load(); runCurrent()
        assertNotNull(editor.state.value.original)
        assertNull(editor.state.value.loadError)
    }
    @Test fun incomeFutureSaveDeleteAndCancellationRespectConfirmation() = runTest {
        val fake = EditingFake()
        var saved = 0
        val editor = IncomeEditor(fake, "synthetic-token", backgroundScope,
            IncomeDraft("Salario", Period(10, 2026), fake.incomes.first()), { error("unexpected") }, { saved++ })
        editor.update { it.copy(amount = "1200,50") }
        editor.save(); runCurrent(); assertEquals(0, fake.writes)
        editor.save(true); runCurrent()
        assertEquals(1200.50, fake.updatedIncome?.second?.amount)
        assertEquals(true, fake.updatedIncome?.second?.update_future)
        assertEquals(1, saved)
        val deletion = IncomeEditor(fake, "synthetic-token", backgroundScope,
            IncomeDraft("Salario", Period(10, 2026), fake.incomes.first(), deleteFuture = true), {}, {})
        deletion.delete(false); runCurrent(); assertNull(fake.deletedIncome)
        deletion.delete(true); runCurrent(); assertEquals(1 to true, fake.deletedIncome)
    }
    @Test fun expiredWriteExitsSessionWithoutSuccessCallback() = runTest {
        val fake = EditingFake().apply { writeFailure = SessionExpiredException() }
        var expired = 0; var saved = 0
        val editor = IncomeEditor(fake, "synthetic-token", backgroundScope,
            IncomeDraft("Renda Extra", Period(10, 2026), amount = "25"), { expired++ }, { saved++ })
        editor.save(); runCurrent()
        assertEquals(1, expired); assertEquals(0, saved)
        assertFalse(editor.mutation.state.value.completed)
    }
    @Test fun cancellationDoesNotBecomeAUserErrorOrSuccessfulWrite() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fake = EditingFake().apply { beforeWrite = { withContext(NonCancellable) { gate.await() } } }
        val job = SupervisorJob()
        val scope = CoroutineScope(backgroundScope.coroutineContext + job)
        var success = 0
        val runner = MutationRunner(scope) { error("unexpected") }
        runner.run({ fake.deleteIncome("synthetic-token", 1, null) }) { success++ }
        runCurrent(); job.cancel(); gate.complete(Unit); runCurrent()
        assertEquals(0, success)
        assertNull(runner.state.value.error)
        assertFalse(runner.state.value.completed)
    }
}
