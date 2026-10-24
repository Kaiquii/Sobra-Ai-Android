package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.perfil.ProfileLoadViewModel
import com.example.appfinanceiro.feature.relatorios.RelatoriosViewModel
import com.example.appfinanceiro.feature.relatorios.InstallmentCommitmentsViewModel
import com.example.appfinanceiro.feature.despesas.DespesasViewModel
import com.example.appfinanceiro.feature.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingRecoveryRegressionTest {
    private fun TestScope.withMain(block: TestScope.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun identicalReportsComparisonsAndCommitmentsReusePendingReads() = runTest { withMain {
        val gate = CompletableDeferred<Unit>()
        var reports = 0; var comparisons = 0; var commitments = 0
        val source = object : ReportsFake() {
            override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
                reports++; gate.await(); return super.getSummary(token, month, year)
            }
            override suspend fun getMonthComparison(token: String, month: Int, year: Int, compareMonth: Int?, compareYear: Int?): MonthComparisonResponse {
                comparisons++; gate.await(); return super.getMonthComparison(token, month, year, compareMonth, compareYear)
            }
            override suspend fun getInstallmentCommitments(token: String, months: Int, month: Int?, year: Int?, includeCurrentMonthAsPaid: Boolean): InstallmentCommitmentsResponse {
                commitments++; gate.await(); return super.getInstallmentCommitments(token, months, month, year, includeCurrentMonthAsPaid)
            }
        }
        val reportsModel = RelatoriosViewModel(source)
        val commitmentsModel = InstallmentCommitmentsViewModel(source)
        try {
            repeat(3) {
                reportsModel.loadReports("synthetic", 10, 2026)
                reportsModel.loadMonthComparison("synthetic", 10, 2026, 9, 2026)
                commitmentsModel.loadCommitments("synthetic", 10, 2026)
                runCurrent()
            }
            assertEquals(listOf(1, 1, 1), listOf(reports, comparisons, commitments))
            gate.complete(Unit); runCurrent()
            assertNotNull(reportsModel.uiState.value.summaryData)
            assertNotNull(reportsModel.uiState.value.monthComparison)
            assertNotNull(commitmentsModel.uiState.value.data)
        } finally { reportsModel.close(); commitmentsModel.close() }
    } }

    @Test fun savedRevisionReplacesPendingReportAndRejectsLateOldResponse() = runTest { withMain {
        val first = CompletableDeferred<Unit>(); val second = CompletableDeferred<Unit>()
        var calls = 0
        val source = object : ReportsFake() {
            override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
                val call = ++calls
                withContext(NonCancellable) { if (call == 1) first.await() else second.await() }
                return super.getSummary(token, month, year).copy(total_expense = call.toDouble())
            }
        }
        val model = RelatoriosViewModel(source)
        try {
            model.loadReports("synthetic", 10, 2026, 1); runCurrent()
            model.loadReports("synthetic", 10, 2026, 2); runCurrent()
            assertEquals(2, calls)
            first.complete(Unit); runCurrent()
            assertNull(model.uiState.value.summaryData)
            assertTrue(model.uiState.value.isLoading)
            second.complete(Unit); runCurrent()
            assertEquals(2.0, model.uiState.value.summaryData?.total_expense)
        } finally { model.close() }
    } }

    @Test fun reportRetryClearsInitialErrorButKeepsWarningForCachedData() = runTest { withMain {
        var fail = true
        var gate = CompletableDeferred<Unit>()
        val source = object : ReportsFake() {
            override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
                if (fail) error("synthetic")
                gate.await(); return super.getSummary(token, month, year)
            }
        }
        val model = RelatoriosViewModel(source)
        try {
            model.loadReports("synthetic", 10, 2026); runCurrent()
            assertNotNull(model.uiState.value.errorMessage)
            fail = false
            model.loadReports("synthetic", 10, 2026); runCurrent()
            assertNull(model.uiState.value.errorMessage)
            assertTrue(model.uiState.value.isLoading)
            gate.complete(Unit); runCurrent()
            val snapshot = model.uiState.value.summaryData
            fail = true
            model.loadReports("synthetic", 10, 2026); runCurrent()
            assertNotNull(model.uiState.value.errorMessage)
            fail = false; gate = CompletableDeferred()
            model.loadReports("synthetic", 10, 2026); runCurrent()
            assertEquals(snapshot, model.uiState.value.summaryData)
            assertNotNull(model.uiState.value.errorMessage)
            assertTrue(model.uiState.value.isLoading)
            gate.complete(Unit); runCurrent()
            assertNull(model.uiState.value.errorMessage)
        } finally { model.close() }
    } }

    @Test fun homeAndExpensesInitialRetriesDoNotReemitOldFailureWhileWaiting() = runTest { withMain {
        val source = EditingFake().apply { readFailure = IllegalStateException("synthetic") }
        val home = HomeViewModel(source); val expenses = DespesasViewModel(source)
        try {
            home.loadExpenses("synthetic", 10, 2026)
            expenses.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertNotNull(home.uiState.value.expensesError)
            assertNotNull(expenses.uiState.value.errorMessage)
            val gate = CompletableDeferred<Unit>()
            source.readFailure = null; source.beforeRead = { gate.await() }
            home.loadExpenses("synthetic", 10, 2026)
            expenses.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertNull(home.uiState.value.expensesError)
            assertNull(expenses.uiState.value.errorMessage)
            assertTrue(home.uiState.value.isExpensesLoading)
            assertTrue(expenses.uiState.value.isLoading)
            gate.complete(Unit); runCurrent()
            assertNull(home.uiState.value.expensesError)
            assertNull(expenses.uiState.value.errorMessage)
        } finally { home.close(); expenses.close() }
    } }

    @Test fun profileReentryReusesPendingReadAndDoesNotAutomaticallyRetryFailure() = runTest { withMain {
        var calls = 0; var fail = true
        val gate = CompletableDeferred<Unit>()
        val model = ProfileLoadViewModel({ calls++; gate.await(); if (fail) error("synthetic") }, {})
        try {
            repeat(3) { model.load("synthetic", false); runCurrent() }
            assertEquals(1, calls)
            gate.complete(Unit); runCurrent()
            assertNotNull(model.state.value.error)
            model.load("synthetic", false); runCurrent()
            assertEquals(1, calls)
            fail = false
            model.load("synthetic", false, retry = true); runCurrent()
            assertEquals(2, calls)
            assertNull(model.state.value.error)
        } finally { model.close() }
    } }

    @Test fun profileRetryKeepsStaleDataWarningAndCloseRejectsLateSuccess() = runTest { withMain {
        var fail = true; var expired = false
        val gate = CompletableDeferred<Unit>()
        val model = ProfileLoadViewModel({
            if (fail) error("synthetic")
            withContext(NonCancellable) { gate.await() }
        }, { expired = true })
        try {
            model.load("synthetic", true); runCurrent()
            assertNotNull(model.state.value.error)
            fail = false
            model.load("synthetic", true, retry = true); runCurrent()
            assertTrue(model.state.value.loading)
            assertNotNull(model.state.value.error)
            model.close(); gate.complete(Unit); runCurrent()
            assertNotNull(model.state.value.error)
            assertFalse(expired)
        } finally { model.close() }
    } }

    @Test fun profileExpirationIsNotReportedAsAnOrdinaryLoadFailure() = runTest { withMain {
        var expired = false
        val model = ProfileLoadViewModel({ throw SessionExpiredException() }, { expired = true })
        try {
            model.load("synthetic", false); runCurrent()
            assertTrue(expired)
            assertNull(model.state.value.error)
        } finally { model.close() }
    } }
}
