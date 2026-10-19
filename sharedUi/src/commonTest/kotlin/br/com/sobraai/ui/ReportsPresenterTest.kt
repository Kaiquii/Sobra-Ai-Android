package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

internal open class FilesFake : FinanceFilesDataSource {
    var exportRequest: ExportRequest? = null
    override suspend fun export(token: String, request: ExportRequest): FinanceFile { exportRequest = request; return FinanceFile("%PDF-synthetic".encodeToByteArray(), "report.pdf", "application/pdf") }
    override suspend fun uploadPhoto(token: String, photo: ProfilePhoto) = ProfilePhotoResponse("ok", "/uploads/a.jpg")
    override suspend fun avatar(token: String, reference: String) = byteArrayOf(1, 2, 3)
}
internal open class ReportsFake : ReportsDataSource {
    val chartYears = mutableListOf<Int>()
    var fail = false
    var badPeriod = false
    override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        if (fail) error("offline")
        return SummaryResponse(if (badPeriod) 12 else month, year, 0.0, 0.0, 0.0, 800.0, 50.0, 0.0, 200.0, 0.0, 0.0, 200.0, 850.0, 1050.0)
    }
    override suspend fun getReportCategories(token: String, month: Int, year: Int) = emptyList<CategoryReportResponse>()
    override suspend fun getReportChart(token: String, year: Int): List<ChartReportResponse> { chartYears += year; return (1..12).map { ChartReportResponse(it, year.toDouble(), it.toDouble()) } }
    override suspend fun getYearlySummary(token: String, year: Int) = YearlySummaryResponse(100.0, 50.0, year)
    override suspend fun getMonthComparison(token: String, month: Int, year: Int, compareMonth: Int?, compareYear: Int?) = MonthComparisonResponse(month, year, compareMonth!!, compareYear!!,
        MonthComparisonSummary(10.0, 8.0, 2.0, 25.0, "aumento", 5.0, 4.0, 1.0, 25.0, "aumento", 5.0, 4.0, 1.0, 25.0, "aumento"))
    override suspend fun getInstallmentCommitments(token: String, months: Int, month: Int?, year: Int?, includeCurrentMonthAsPaid: Boolean) =
        InstallmentCommitmentsResponse(month!!, year!!, months, InstallmentCommitmentsSummary(0.0, 0.0, 0.0, 0, 0, 0), emptyList(), emptyList())
}
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsPresenterTest {
    @Test fun rollingChartCrossesYearBoundaryWithoutRelabelingAnnualData() = runTest {
        val source = ReportsFake()
        val p = ReportsPresenter(source, FilesFake(), NativeFake(), "synthetic", backgroundScope, Period(2, 2026), {})
        p.reports.open(); runCurrent()
        val snapshot = assertNotNull(p.reports.state.value.data)
        assertEquals(listOf(Period(9, 2025), Period(10, 2025), Period(11, 2025), Period(12, 2025), Period(1, 2026), Period(2, 2026)), snapshot.value.chart.map { it.first })
        assertEquals(listOf(2025, 2026), source.chartYears.sorted()); assertEquals(2025.0, snapshot.value.chart.first().second.income)
        assertEquals(850.0, snapshot.value.summary.total_geral_disponivel)
    }
    @Test fun failureRetainsLabeledOldSnapshotUntilSuccessfulRetry() = runTest {
        val source = ReportsFake(); val p = ReportsPresenter(source, FilesFake(), NativeFake(), "synthetic", backgroundScope, Period(10, 2026), {})
        p.reports.open(); runCurrent(); source.fail = true
        p.reports.select(ReportsKey(Period(11, 2026))); runCurrent()
        assertEquals(Period(10, 2026), p.reports.state.value.data?.key?.period)
        assertEquals(Period(11, 2026), p.reports.state.value.key.period); assertNotNull(p.reports.state.value.error)
        source.fail = false; p.reports.refresh(); assertNotNull(p.reports.state.value.error); runCurrent()
        assertEquals(Period(11, 2026), p.reports.state.value.data?.key?.period); assertNull(p.reports.state.value.error)
    }
    @Test fun mismatchedPeriodIsRejectedRatherThanDisplayed() = runTest {
        val source = ReportsFake().also { it.badPeriod = true }
        val p = ReportsPresenter(source, FilesFake(), NativeFake(), "synthetic", backgroundScope, Period(10, 2026), {})
        p.reports.open(); runCurrent(); assertNull(p.reports.state.value.data); assertNotNull(p.reports.state.value.error)
    }
    @Test fun exportSharesOnlyDownloadedFileAndGuardsDuplicateTap() = runTest {
        val files = FilesFake(); val native = NativeFake()
        val p = ReportsPresenter(ReportsFake(), files, native, "synthetic", backgroundScope, Period(10, 2026), {})
        val request = ExportRequest(ExportKind.Commitments, 10, 2026, ExportFormat.Pdf, months = 24, includeCurrentAsPaid = true)
        p.export(request); p.export(request); runCurrent()
        assertEquals(request, files.exportRequest); assertEquals("report.pdf", native.shared?.fileName); assertNull(p.exportAction.state.value.error)
    }
    @Test fun filteringExpiredSeriesRecomputesTotalsWithoutChangingFinancialValues() {
        fun purchase(series: String, lastMonth: Int, lastYear: Int) = InstallmentPurchase(series, "Teste", 1, "Casa", "Salario", 50.0, 100.0, 50.0, 50.0, 1, 1, 2, 9, 2026, lastMonth, lastYear)
        fun parcel(series: String, amount: Double) = InstallmentParcel(1, series, "Teste", 1, "Casa", "Salario", amount, 10, 2026, 2, 2)
        val data = InstallmentCommitmentsResponse(10, 2026, 12, InstallmentCommitmentsSummary(999.0, 999.0, 999.0, 9, 9, 9),
            listOf(purchase("active", 10, 2026), purchase("expired", 9, 2026), purchase("next-year", 1, 2027)),
            listOf(InstallmentTimelineMonth(10, 2026, 999.0, listOf(parcel("active", 50.0), parcel("expired", 20.0), parcel("next-year", 12.5)))))
        val filtered = data.activeCommitments()
        assertEquals(listOf("active", "next-year"), filtered.compras.map { it.serie_id })
        assertEquals(62.5, filtered.linha_do_tempo.single().total); assertEquals(200.0, filtered.resumo.total_original)
        assertEquals(100.0, filtered.resumo.total_restante); assertEquals(62.5, filtered.resumo.mes_mais_pesado?.total)
    }
    @Test fun genericQueryIgnoresLateResponseAfterSelectingNewKey() = runTest {
        val old = CompletableDeferred<String>()
        val query = FeatureQuery(1, backgroundScope, {}) { key: Int -> if (key == 1) withContext(NonCancellable) { old.await() } else "new" }
        query.open(); runCurrent(); query.select(2); runCurrent(); old.complete("old"); runCurrent()
        assertEquals(LoadedValue(2, "new"), query.state.value.data)
    }
}
