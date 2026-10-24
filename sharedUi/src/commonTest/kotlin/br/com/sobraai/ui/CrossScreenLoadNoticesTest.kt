package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.home.*
import com.example.appfinanceiro.feature.relatorios.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CrossScreenLoadNoticesTest {
    @Test fun homeGroupsConnectionFailuresAndKeepsRemainingFailuresAfterPartialRecovery() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = EditingFake()
        val model = HomeViewModel(fake)
        try {
            model.loadAll("synthetic", 10, 2026); runCurrent()
            fake.readFailure = NetworkRequestException("Falha sintética", IllegalStateException("synthetic"))
            model.loadAll("synthetic", 10, 2026); runCurrent()
            val notice = homeLoadNotices(model.uiState.value).single()
            assertEquals(setOf(HomeLoadSource.Summary, HomeLoadSource.Expenses, HomeLoadSource.Categories), notice.sources)
            assertTrue(notice.message.contains("dados salvos"))
            fake.readFailure = null
            model.loadCategories("synthetic"); runCurrent()
            assertEquals(setOf(HomeLoadSource.Summary, HomeLoadSource.Expenses), homeLoadNotices(model.uiState.value).single().sources)
            model.loadAll("synthetic", 10, 2026); runCurrent()
            assertTrue(homeLoadNotices(model.uiState.value).isEmpty())
            assertNull(model.uiState.value.summaryNetworkFailure)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun reportsAndComparisonUseOneNoticeAndCanRecoverIndependently() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var fail = false
        val source = object : ReportsFake() {
            override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
                if (fail) throw NetworkRequestException("Demora sintética", IllegalStateException("synthetic"), NetworkFailureKind.Timeout)
                return super.getSummary(token, month, year)
            }
            override suspend fun getMonthComparison(token: String, month: Int, year: Int, compareMonth: Int?, compareYear: Int?): MonthComparisonResponse {
                if (fail) throw NetworkRequestException("Demora sintética", IllegalStateException("synthetic"), NetworkFailureKind.Timeout)
                return super.getMonthComparison(token, month, year, compareMonth, compareYear)
            }
        }
        val model = RelatoriosViewModel(source)
        try {
            model.loadReports("synthetic", 10, 2026); model.loadMonthComparison("synthetic", 10, 2026, 9, 2026); runCurrent()
            fail = true
            model.loadReports("synthetic", 10, 2026); model.loadMonthComparison("synthetic", 10, 2026, 9, 2026); runCurrent()
            assertEquals(setOf(ReportsLoadSource.Reports, ReportsLoadSource.Comparison), reportsLoadNotices(model.uiState.value).single().sources)
            assertTrue(reportsLoadNotices(model.uiState.value).single().message.contains("demorou"))
            fail = false
            model.loadReports("synthetic", 10, 2026); runCurrent()
            assertEquals(setOf(ReportsLoadSource.Comparison), reportsLoadNotices(model.uiState.value).single().sources)
            model.loadMonthComparison("synthetic", 10, 2026, 9, 2026); runCurrent()
            assertTrue(reportsLoadNotices(model.uiState.value).isEmpty())
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun apiErrorsStaySeparateInBothHomeAndReportsEvenWhenMessagesMentionConnection() {
        val apiError = "Não foi possível conectar ao banco do serviço."
        val home = homeLoadNotices(HomeUiState(summaryError = apiError, categoriesError = apiError))
        val reports = reportsLoadNotices(RelatoriosUiState(errorMessage = apiError, comparisonErrorMessage = apiError))
        assertEquals(2, home.size)
        assertEquals(2, reports.size)
        assertTrue((home.map { it.message } + reports.map { it.message }).all { it.contains(apiError) })
    }
}
