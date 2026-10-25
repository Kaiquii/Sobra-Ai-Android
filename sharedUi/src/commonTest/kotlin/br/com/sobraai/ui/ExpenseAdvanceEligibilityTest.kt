package br.com.sobraai.ui

import com.example.appfinanceiro.core.network.supportsAdvance
import com.example.appfinanceiro.core.network.supportsExpenseAdvance
import com.example.appfinanceiro.feature.despesas.DespesasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseAdvanceEligibilityTest {
    @Test fun onlyUniqueAndInstallmentTypesAllowAdvancingRegardlessOfSourceOrPaidStatus() {
        for (type in listOf("Única", "Unica", "única", " UNICA ", "Parcelada", "parcelada")) {
            assertTrue(supportsExpenseAdvance(type), type)
            for (source in listOf("Salário", "Adiantamento", "Renda Extra", "VA", "VR")) {
                assertTrue(testExpense(type = type).copy(payment_source = source, is_paid = true).supportsAdvance)
            }
        }
        for (type in listOf("Fixa", "fixa", " FIXA ", "", "Desconhecida")) {
            assertFalse(supportsExpenseAdvance(type), type)
            assertFalse(testExpense(type = type, advanced = true).supportsAdvance)
        }
    }

    @Test fun fixedExpenseCannotSendAdvanceMutationsButCanStillBeMarkedPaid() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = EditingFake()
        val model = DespesasViewModel(fake)
        var completed = 0
        val fixed = testExpense(type = "Fixa")
        try {
            for (advanced in listOf(true, false)) {
                model.updateAdvanceStatus("synthetic", fixed.copy(isAdvanced = !advanced), advanced,
                    if (advanced) "2026-09-30" else null) { completed++ }
                runCurrent()
                assertEquals(0, fake.writes)
                assertEquals(0, completed)
                assertNull(fake.advanced)
                assertNotNull(model.uiState.value.advanceStatusErrorMessage)
                assertNull(model.uiState.value.advanceStatusSuccessMessage)
                assertFalse(model.uiState.value.isUpdatingAdvanceStatus)
            }
            model.updateExpensePaymentStatus("synthetic", fixed, true) { completed++ }
            runCurrent()
            assertEquals(1 to true, fake.paid)
            assertEquals(1, completed)
            assertNull(fake.advanced)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun uniqueAndInstallmentExpensesCanAdvanceAndRemoveAdvanceWithoutChangingPaidStatus() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = EditingFake()
        val model = DespesasViewModel(fake)
        try {
            for (type in listOf("Única", "Parcelada")) {
                val expense = testExpense(type = type).copy(is_paid = true)
                fake.expense = expense
                var completed = 0
                model.updateAdvanceStatus("synthetic", expense, true, "2026-09-30") { completed++ }
                runCurrent()
                assertEquals(Triple(expense.id, true, "2026-09-30"), fake.advanced)
                model.updateAdvanceStatus("synthetic", expense.copy(isAdvanced = true), false, null) { completed++ }
                runCurrent()
                assertEquals(Triple(expense.id, false, null), fake.advanced)
                assertEquals(2, completed)
                assertNull(fake.paid)
                assertNull(model.uiState.value.advanceStatusErrorMessage)
            }
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun workspaceRejectsFixedExpenseWithoutHttpMutation() = runTest {
        val fake = EditingFake()
        val home = HomePresenter(fake, "synthetic", backgroundScope, Period(10, 2026)) {}
        val workspace = FinanceWorkspace(home, fake, fake, "synthetic", backgroundScope) {}
        try {
            for (date in listOf(FinancialDate(2026, 9, 30), null)) {
                val runner = workspace.newAction()
                workspace.advance(testExpense(type = "Fixa", advanced = date == null), date, runner)
                runCurrent()
                assertEquals(0, fake.writes)
                assertNotNull(runner.state.value.error)
                assertFalse(runner.state.value.completed)
            }
        } finally { workspace.close() }
    }
}
