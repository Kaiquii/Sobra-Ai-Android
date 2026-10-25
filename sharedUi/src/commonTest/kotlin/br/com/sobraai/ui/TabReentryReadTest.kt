package br.com.sobraai.ui

import br.com.sobraai.ui.runtime.PresentationModel
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.feature.despesas.DespesasViewModel
import com.example.appfinanceiro.feature.home.HomeViewModel
import com.example.appfinanceiro.feature.perfil.ProfileLoadViewModel
import com.example.appfinanceiro.feature.perfil.SettingsListLoadViewModel
import com.example.appfinanceiro.feature.relatorios.InstallmentCommitmentsViewModel
import com.example.appfinanceiro.feature.relatorios.RelatoriosViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.TestTimeSource
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, kotlin.time.ExperimentalTime::class)
class TabReentryReadTest {
    private fun TestScope.withMain(block: TestScope.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun quickTabReentryDoesNotRepeatCompletedFinancialReads() = runTest { withMain {
        val data = EditingFake()
        val reports = ReportsFake()
        val home = HomeViewModel(data)
        val expenses = DespesasViewModel(data)
        val report = RelatoriosViewModel(reports)
        val commitments = InstallmentCommitmentsViewModel(reports)
        var profileReads = 0
        val profile = ProfileLoadViewModel({ profileReads++ }, {})
        try {
            repeat(3) {
                home.loadAll("synthetic", 10, 2026, reuseRecent = true)
                expenses.loadExpenses("synthetic", 10, 2026, reuseRecent = true)
                report.loadReports("synthetic", 10, 2026, reuseRecent = true)
                report.loadMonthComparison("synthetic", 10, 2026, 9, 2026, reuseRecent = true)
                commitments.loadCommitments("synthetic", 10, 2026, reuseRecent = true)
                profile.load("synthetic", true)
                runCurrent()
            }
            assertEquals(1, data.summaryReads)
            assertEquals(1, data.scheduledReads)
            assertEquals(2, data.effectiveReads) // One per retained screen model.
            assertEquals(listOf(2026), reports.chartYears)
            assertEquals(1, profileReads)
            assertNotNull(report.uiState.value.monthComparison)
            assertNotNull(commitments.uiState.value.data)
            assertFalse(home.uiState.value.isSummaryLoading)
            assertFalse(expenses.uiState.value.isLoading)
        } finally { listOf(home, expenses, report, commitments, profile).forEach { it.close() } }
    } }

    @Test fun savedRevisionPeriodAndFilterBypassRecentReadReuse() = runTest { withMain {
        val data = EditingFake()
        val model = HomeViewModel(data)
        val reports = ReportsFake()
        val report = RelatoriosViewModel(reports)
        try {
            model.loadAll("synthetic", 10, 2026, refreshKey = 1, reuseRecent = true); runCurrent()
            model.loadAll("synthetic", 10, 2026, refreshKey = 2, reuseRecent = true); runCurrent()
            assertEquals(2, data.summaryReads)
            model.loadExpenses("synthetic", 10, 2026, "paid", 2, reuseRecent = true); runCurrent()
            assertEquals(3, data.effectiveReads)
            model.loadAll("synthetic", 11, 2026, refreshKey = 2, reuseRecent = true); runCurrent()
            assertEquals(11, model.uiState.value.summaryData?.month)
            report.loadReports("synthetic", 10, 2026, 1, reuseRecent = true); runCurrent()
            report.loadReports("synthetic", 10, 2026, 2, reuseRecent = true); runCurrent()
            report.loadReports("synthetic", 11, 2026, 2, reuseRecent = true); runCurrent()
            assertEquals(3, reports.chartYears.size)
            assertEquals(11, report.uiState.value.summaryData?.month)
        } finally { model.close(); report.close() }
    } }

    @Test fun realConnectionFailureStaysVisibleAndManualRetryStillReads() = runTest { withMain {
        val data = EditingFake()
        val model = DespesasViewModel(data)
        try {
            model.loadExpenses("synthetic", 10, 2026, reuseRecent = true); runCurrent()
            data.readFailure = NetworkRequestException("Falha sintética", IllegalStateException("synthetic"))
            model.loadExpenses("synthetic", 10, 2026); runCurrent() // Explicit refresh.
            val failure = assertNotNull(model.uiState.value.errorMessage)
            assertEquals(2, data.scheduledReads)
            model.loadExpenses("synthetic", 10, 2026, reuseRecent = true); runCurrent()
            assertEquals(2, data.scheduledReads)
            assertEquals(failure, model.uiState.value.errorMessage)
            assertTrue(model.uiState.value.effectiveExpensesData.isNotEmpty())
            data.readFailure = null
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertEquals(3, data.scheduledReads)
            assertNull(model.uiState.value.errorMessage)
            assertNull(model.uiState.value.categoriesError)
        } finally { model.close() }
    } }

    @Test fun payingAdvancingAndDeletingInvalidateRecentReportsOnlyAfterSuccessfulWrite() = runTest { withMain {
        val data = EditingFake().apply { expense = testExpense(type = "Única") }
        val expenses = DespesasViewModel(data)
        val source = ReportsFake()
        val reports = RelatoriosViewModel(source)
        fun enterReports() = reports.loadReports("synthetic", 10, 2026,
            refreshKey = FinanceDataChanges.revision.value, reuseRecent = true)
        try {
            enterReports(); runCurrent()
            expenses.updateExpensePaymentStatus("synthetic", data.expense, true) {}; runCurrent()
            enterReports(); runCurrent()
            expenses.updateAdvanceStatus("synthetic", data.expense, true, "2026-10-01") {}; runCurrent()
            enterReports(); runCurrent()
            expenses.deleteExpense("synthetic", data.expense.id, null) {}; runCurrent()
            enterReports(); runCurrent()
            assertEquals(4, source.chartYears.size)
            val revision = FinanceDataChanges.revision.value
            data.writeFailure = IllegalStateException("synthetic")
            expenses.deleteExpense("synthetic", data.expense.id, null) {}; runCurrent()
            enterReports(); runCurrent()
            assertEquals(revision, FinanceDataChanges.revision.value)
            assertEquals(4, source.chartYears.size)
            assertNotNull(expenses.uiState.value.deleteErrorMessage)
        } finally { expenses.close(); reports.close() }
    } }

    private class Probe(val clock: TestTimeSource) : PresentationModel(clock) {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        fun read(request: Int = 1, reuseRecent: Boolean = true) =
            launchLatest("read", request, reuseRecent) { calls++; gate?.await() }
    }

    @Test fun categoryAndIncomeSettingsRetainEmptyResultsAndRetryOnlyOnNewRevision() = runTest { withMain {
        for (items in listOf(emptyList(), listOf("Sintético"))) {
            val model = SettingsListLoadViewModel<String>()
            var calls = 0
            var fail = false
            val read: suspend () -> List<String> = {
                calls++
                if (fail) throw NetworkRequestException("Falha sintética", IllegalStateException("synthetic"))
                items
            }
            try {
                repeat(3) { model.load("synthetic", 1, "Falha", read); runCurrent() }
                assertEquals(1, calls)
                assertEquals(items, model.state.value.items)
                assertTrue(model.state.value.hasLoadedOnce)
                fail = true
                model.load("synthetic", 2, "Falha", read); runCurrent()
                assertNotNull(model.state.value.error)
                model.load("synthetic", 2, "Falha", read); runCurrent()
                assertEquals(2, calls)
                assertNotNull(model.state.value.error)
                fail = false
                model.load("synthetic", 3, "Falha", read); runCurrent()
                assertEquals(3, calls)
                assertNull(model.state.value.error)
            } finally { model.close() }
        }
    } }

    @Test fun leavingSettingsDoesNotCancelPendingReadAndNewRevisionRejectsLateOldData() = runTest { withMain {
        val model = SettingsListLoadViewModel<String>()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        try {
            val read: suspend () -> List<String> = {
                calls++
                withContext(NonCancellable) { gate.await() }
                listOf("Antigo")
            }
            model.load("synthetic", 1, "Falha", read); runCurrent()
            model.load("synthetic", 1, "Falha", read); runCurrent()
            assertEquals(1, calls)
            model.load("synthetic", 2, "Falha") { listOf("Atual") }; runCurrent()
            gate.complete(Unit); runCurrent()
            assertEquals(listOf("Atual"), model.state.value.items)
            assertNull(model.state.value.error)
        } finally { model.close() }
    } }

    @Test fun reuseExpiresFromCompletionAndExplicitRefreshIsNeverCachedForever() = runTest { withMain {
        val model = Probe(TestTimeSource())
        try {
            model.gate = CompletableDeferred()
            model.read(); runCurrent()
            model.clock += 120.seconds
            model.read(); runCurrent()
            assertEquals(1, model.calls) // Still pending, not stale.
            model.gate!!.complete(Unit); runCurrent()
            model.clock += 59.seconds
            model.read(); runCurrent(); assertEquals(1, model.calls)
            model.clock += 1.seconds
            model.read(); runCurrent(); assertEquals(2, model.calls)
            model.read(reuseRecent = false); runCurrent(); assertEquals(3, model.calls)
            model.read(request = 2); runCurrent(); assertEquals(4, model.calls)
        } finally { model.close() }
    } }

    @Test fun cancelledReadIsNotReusedAsCompletedAndClosingSessionCancelsPendingWork() = runTest { withMain {
        val model = Probe(TestTimeSource())
        try {
            model.gate = CompletableDeferred()
            val first = model.read(); runCurrent(); first.cancel(); runCurrent()
            model.read(); runCurrent(); assertEquals(2, model.calls)
            model.close()
            model.gate!!.complete(Unit); runCurrent()
            assertFalse(model.viewModelScope.isActive)
        } finally { model.close() }
    } }
}
