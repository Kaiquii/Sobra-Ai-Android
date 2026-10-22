package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.ReportsDataSource
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*

data class ReportsKey(val period: Period, val compare: Period = period.shift(-1), val chartMonths: Int = 6)
data class ReportsBundle(val summary: SummaryResponse, val categories: List<CategoryReportResponse>,
    val chart: List<Pair<Period, ChartReportResponse>>, val yearly: YearlySummaryResponse, val comparison: MonthComparisonResponse,
    val displayChart: List<ChartReportResponse> = chart.map { it.second })
data class CommitmentsKey(val period: Period, val months: Int = 12, val includeCurrentAsPaid: Boolean = false)

class ReportsPresenter(source: ReportsDataSource, private val files: FinanceFilesDataSource, private val platform: NativeFeatures,
    private val token: String, scope: CoroutineScope, period: Period, onExpired: () -> Unit) {
    val reports = FeatureQuery(ReportsKey(period), scope, onExpired) { key -> coroutineScope {
        require(key.chartMonths in listOf(1, 6, 12))
        val summary = async { source.getSummary(token, key.period.month, key.period.year) }
        val categories = async { source.getReportCategories(token, key.period.month, key.period.year) }
        val yearly = async { source.getYearlySummary(token, key.period.year) }
        val compare = async { source.getMonthComparison(token, key.period.month, key.period.year, key.compare.month, key.compare.year) }
        val periods = (key.chartMonths - 1 downTo 0).map { key.period.shift(-it) }
        val charts = periods.map { it.year }.distinct().associateWith { year -> async { source.getReportChart(token, year) } }
        val chart = periods.map { p -> p to (charts.getValue(p.year).await().firstOrNull { it.month == p.month } ?: ChartReportResponse(p.month, 0.0, 0.0)) }
        val s = summary.await(); val y = yearly.await(); val c = compare.await()
        require(s.month == key.period.month && s.year == key.period.year && y.year == key.period.year &&
            c.mes_atual == key.period.month && c.ano_atual == key.period.year && c.mes_comparado == key.compare.month && c.ano_comparado == key.compare.year) {
            "O relatório recebido não corresponde aos períodos solicitados."
        }
        ReportsBundle(s, categories.await(), chart, y, c, charts.getValue(key.period.year).await())
    } }
    val commitments = FeatureQuery(CommitmentsKey(period), scope, onExpired) { key ->
        val value = source.getInstallmentCommitments(token, key.months, key.period.month, key.period.year, key.includeCurrentAsPaid)
        require(value.mes_base == key.period.month && value.ano_base == key.period.year) { "Os compromissos recebidos são de outro período." }
        value.activeCommitments()
    }
    val exportAction = FeatureAction(scope, onExpired)
    fun export(request: ExportRequest) { exportAction.run { val file = files.export(token, request); platform.share(file); "Relatório gerado." } }
    fun invalidate() { reports.refreshIfLoaded(); commitments.refreshIfLoaded() }
}

/** Same active-series projection used by the Android commitments screen. */
internal fun InstallmentCommitmentsResponse.activeCommitments(): InstallmentCommitmentsResponse {
    val base = ano_base * 12 + mes_base - 1
    val purchases = compras.filter { it.ultimo_ano * 12 + it.ultimo_mes - 1 >= base }
    val series = purchases.mapTo(hashSetOf()) { it.serie_id }
    val timeline = linha_do_tempo.map { month ->
        val parcels = month.parcelas.filter { it.serie_id in series }
        month.copy(parcelas = parcels, total = parcels.sumOf { it.valor })
    }
    val heavy = timeline.filter { it.total > 0 }.maxByOrNull { it.total }?.let { InstallmentHeavyMonth(it.mes, it.ano, it.total) }
    return copy(compras = purchases, linha_do_tempo = timeline, resumo = InstallmentCommitmentsSummary(
        purchases.sumOf { it.total_original }, purchases.sumOf { it.total_pago }, purchases.sumOf { it.total_restante },
        purchases.sumOf { it.parcelas_pagas }, purchases.sumOf { it.parcelas_restantes }, purchases.size, heavy))
}
