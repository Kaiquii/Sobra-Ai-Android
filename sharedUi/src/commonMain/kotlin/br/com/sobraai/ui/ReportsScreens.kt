package br.com.sobraai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.components.*
import com.example.appfinanceiro.core.network.*
import kotlin.math.max

@Composable
internal fun ReportsScreen(workspace: FinanceWorkspace, presenter: ReportsPresenter, hidden: Boolean, toggle: () -> Unit) {
    val state by presenter.reports.state.collectAsState()
    var export by remember { mutableStateOf(false) }
    val key = state.key
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Relatórios", { workspace.navigate(FinanceRoute.Home) }) {
            if (state.loading && state.data != null) AppLoadingIndicator(Modifier.padding(8.dp))
            PrivacyIcon(hidden, toggle)
            IconButton(onClick = presenter.reports::refresh, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Atualizar relatórios") }
        } }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Reports) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PeriodSelector(key.period) { presenter.reports.select(key.copy(period = it)) } }
            item { QueryFeedback(state.loading, state.error, state.data?.key?.description, key.description, presenter.reports::refresh) }
            item { Button(onClick = { presenter.exportAction.reset(); export = true }, enabled = !hidden && state.data != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Exportar relatório") } }
            item { OutlinedButton(onClick = { workspace.navigate(FinanceRoute.Commitments) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Compromissos parcelados") } }
            if (hidden) item { PrivateFeaturePlaceholder() }
            else state.data?.value?.let { data ->
                item { FinancialPanel("Resumo de ${state.data!!.key.period.label}", listOf(
                    "Receitas" to data.summary.total_income, "Despesas" to data.summary.total_expense, "Saldo disponível" to data.summary.total_geral_disponivel)) }
                item { ChoiceField("Período do gráfico", "${key.chartMonths} ${if (key.chartMonths == 1) "mês" else "meses"}", listOf("1 mês", "6 meses", "1 ano")) {
                    presenter.reports.select(key.copy(chartMonths = listOf(1, 6, 12)[it]))
                } }
                item { Text("Receitas e despesas", style = MaterialTheme.typography.titleMedium); IncomeExpenseChart(data.chart.map { it.second }) }
                items(data.chart, key = { "chart-${it.first.year}-${it.first.month}" }) { (period, month) ->
                    FinancialPanel(period.label, listOf("Receitas" to month.income, "Despesas" to month.expense))
                }
                item { Text("Despesas por categoria", style = MaterialTheme.typography.titleMedium) }
                if (data.categories.isEmpty()) item { Text("Nenhuma despesa por categoria neste mês.") }
                items(data.categories, key = { "category-${it.category_id}" }) { category ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(category.category_name, fontWeight = FontWeight.Bold); FinancialValue(formatCurrency(category.total_amount), false)
                        Text("${category.percentage.toString().replace('.', ',')}% das despesas")
                        LinearProgressIndicator(progress = { (category.percentage / 100).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().clearAndSetSemantics {})
                    } }
                }
                item { FinancialPanel("Resumo anual ${data.yearly.year}", listOf("Economia total" to data.yearly.economia_total, "Média mensal" to data.yearly.media_mensal)) }
                item { Text("Comparar com", style = MaterialTheme.typography.titleMedium)
                    PeriodSelector(key.compare) { presenter.reports.select(key.copy(compare = it)) } }
                item { val summary = data.comparison.resumo
                    Text("${state.data!!.key.period.label} × ${state.data!!.key.compare.label}")
                    FinancialPanel("Comparativo de receitas", listOf("Atual" to summary.receitas_atual, "Comparado" to summary.receitas_comparado, "Diferença" to summary.diferenca_receitas))
                    ComparisonChange(summary.percentual_receitas, summary.status_receitas)
                    FinancialPanel("Comparativo de despesas", listOf("Atual" to summary.despesas_atual, "Comparado" to summary.despesas_comparado, "Diferença" to summary.diferenca_despesas))
                    ComparisonChange(summary.percentual_despesas, summary.status_despesas)
                    FinancialPanel("Comparativo de saldo", listOf("Atual" to summary.saldo_atual, "Comparado" to summary.saldo_comparado, "Diferença" to summary.diferenca_saldo))
                    ComparisonChange(summary.percentual_saldo, summary.status_saldo)
                }
                items(data.comparison.categorias, key = { "compare-category-${it.categoria_id}" }) {
                    FinancialPanel(it.categoria_nome, listOf("Atual" to it.valor_atual, "Comparado" to it.valor_comparado, "Diferença" to it.diferenca))
                    ComparisonChange(it.percentual, it.status)
                }
                items(data.comparison.fontes_pagamento) { FinancialPanel(it.fonte_pagamento, listOf("Atual" to it.valor_atual, "Comparado" to it.valor_comparado, "Diferença" to it.diferenca)); ComparisonChange(it.percentual, it.status) }
                items(data.comparison.tipos_despesa) { FinancialPanel(it.tipo, listOf("Atual" to it.valor_atual, "Comparado" to it.valor_comparado, "Diferença" to it.diferenca)); ComparisonChange(it.percentual, it.status) }
                items(data.comparison.insights) { Text(it) }
            } ?: item { Text("Os relatórios serão exibidos após o carregamento.") }
        }
    }
    if (export) ReportExportDialog(presenter, key.period, key.compare, { export = false })
}

@Composable
internal fun CommitmentsScreen(workspace: FinanceWorkspace, presenter: ReportsPresenter, hidden: Boolean, toggle: () -> Unit) {
    val state by presenter.commitments.state.collectAsState()
    val key = state.key
    var tab by remember { mutableIntStateOf(0) }
    var export by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Compromissos Parcelados", { workspace.navigate(FinanceRoute.Reports) }) { PrivacyIcon(hidden, toggle) } },
        bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Commitments) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PeriodSelector(key.period) { presenter.commitments.select(key.copy(period = it)) } }
            item { ChoiceField("Horizonte", "${key.months} meses", listOf("6 meses", "12 meses", "24 meses")) { presenter.commitments.select(key.copy(months = listOf(6, 12, 24)[it])) } }
            item { FormCheck("Considerar o mês base como pago na projeção", key.includeCurrentAsPaid,
                { presenter.commitments.select(key.copy(includeCurrentAsPaid = it)) }) }
            item { QueryFeedback(state.loading, state.error, state.data?.key?.description, key.description, presenter.commitments::refresh) }
            item { OutlinedButton(onClick = { presenter.exportAction.reset(); export = true }, enabled = !hidden && state.data != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Exportar compromissos") } }
            item { ChoiceField("Visão", listOf("Compras", "Linha do tempo", "Calendário")[tab], listOf("Compras", "Linha do tempo", "Calendário")) { tab = it } }
            if (hidden) item { PrivateFeaturePlaceholder() }
            else state.data?.value?.let { data ->
                item { FinancialPanel("Compromissos de ${state.data!!.key.period.label}", listOf("Total original" to data.resumo.total_original,
                    "Total pago" to data.resumo.total_pago, "Total restante" to data.resumo.total_restante))
                    Text("${data.resumo.total_compras} compras • ${data.resumo.parcelas_pagas} parcelas pagas • ${data.resumo.parcelas_restantes} restantes")
                    Text(state.data!!.key.description)
                    data.resumo.mes_mais_pesado?.let { Text("Mês mais pesado: ${periodLabel(it.mes, it.ano)}"); FinancialValue(formatCurrency(it.total), false) }
                }
                if (data.compras.isEmpty()) item { Text("Nenhuma compra parcelada ativa neste período.") }
                if (tab == 0) items(data.compras, key = { it.serie_id }) { purchase ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(purchase.descricao, fontWeight = FontWeight.Bold); Text("${purchase.categoria_nome} • ${purchase.fonte_pagamento}")
                        FinancialValue("Parcela: ${formatCurrency(purchase.valor_parcela)}", false)
                        FinancialValue("Pago: ${formatCurrency(purchase.total_pago)} • Restante: ${formatCurrency(purchase.total_restante)}", false)
                        Text("${purchase.parcelas_pagas}/${purchase.total_parcelas} pagas • ${purchase.parcelas_restantes} restantes")
                        Text("${periodLabel(purchase.primeiro_mes, purchase.primeiro_ano)} até ${periodLabel(purchase.ultimo_mes, purchase.ultimo_ano)}")
                        purchase.proxima_parcela?.let { Text("Próxima: ${periodLabel(it.mes, it.ano)} • ${it.parcela_atual}/${it.total_parcelas}"); FinancialValue(formatCurrency(it.valor), false) }
                    } }
                } else items(data.linha_do_tempo, key = { "${it.ano}-${it.mes}" }) { month ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(periodLabel(month.mes, month.ano), fontWeight = FontWeight.Bold)
                        FinancialValue(formatCurrency(month.total), false)
                        if (tab == 2) Text("${month.parcelas.size} parcelas no mês")
                        month.parcelas.forEach { Text("${it.descricao} • ${it.parcela_atual}/${it.total_parcelas}"); FinancialValue(formatCurrency(it.valor), false) }
                    } }
                }
            }
        }
    }
    if (export) ReportExportDialog(presenter, key.period, key.period.shift(-1), { export = false }, ExportKind.Commitments, key.months, key.includeCurrentAsPaid)
}

private val ReportsKey.description: String get() = "${period.label}, gráfico de $chartMonths meses, comparado com ${compare.label}"
private val CommitmentsKey.description: String get() = "${period.label}, horizonte de $months meses, mês base ${if (includeCurrentAsPaid) "considerado pago" else "sem simular pagamento"}"

@Composable
private fun ReportExportDialog(presenter: ReportsPresenter, period: Period, comparison: Period, dismiss: () -> Unit,
    initialKind: ExportKind = ExportKind.Full, months: Int = 12, includePaid: Boolean = false) {
    var kind by remember { mutableStateOf(initialKind) }; var format by remember { mutableStateOf(ExportFormat.Pdf) }
    var compare by remember { mutableStateOf(comparison) }; var horizon by remember { mutableIntStateOf(months) }; var paid by remember { mutableStateOf(includePaid) }
    val action by presenter.exportAction.state.collectAsState()
    AlertDialog(onDismissRequest = { if (!action.busy) dismiss() }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Exportar relatório") }, text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(period.label)
                ChoiceField("Conteúdo", kind.label, ExportKind.entries.map { it.label }, !action.busy) { kind = ExportKind.entries[it] }
                ChoiceField("Formato", format.label, ExportFormat.entries.map { it.label }, !action.busy) { format = ExportFormat.entries[it] }
                if (kind in listOf(ExportKind.Comparison, ExportKind.Full)) { Text("Comparar com"); PeriodSelector(compare) { if (!action.busy) compare = it } }
                if (kind in listOf(ExportKind.Commitments, ExportKind.Full)) {
                    ChoiceField("Horizonte", "$horizon meses", listOf("6 meses", "12 meses", "24 meses"), !action.busy) { horizon = listOf(6, 12, 24)[it] }
                    FormCheck("Considerar mês base como pago na projeção", paid, { paid = it }, !action.busy)
                }
                FeatureActionFeedback(action)
            }
        }, confirmButton = { TextButton(onClick = {
            val comparisonEnabled = kind in listOf(ExportKind.Comparison, ExportKind.Full)
            val commitmentsEnabled = kind in listOf(ExportKind.Commitments, ExportKind.Full)
            presenter.export(ExportRequest(kind, period.month, period.year, format,
                if (comparisonEnabled) compare.month else null, if (comparisonEnabled) compare.year else null,
                if (commitmentsEnabled) horizon else null, if (commitmentsEnabled) paid else null))
        }, enabled = !action.busy) { Text("Gerar e compartilhar") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !action.busy) { Text("Fechar") } })
}

@Composable
internal fun FeatureActionFeedback(state: FeatureActionState) {
    if (state.busy) AppLoadingIndicator()
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    state.message?.let { Text(it) }
}
@Composable
private fun ComparisonChange(percentage: Double, status: String) {
    Text("Variação: ${percentage.toString().replace('.', ',')}% • ${status.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall)
}
@Composable
internal fun PrivacyIcon(hidden: Boolean, toggle: () -> Unit) { IconButton(onClick = toggle) { Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (hidden) "Mostrar valores" else "Ocultar valores") } }
@Composable
internal fun QueryFeedback(loading: Boolean, error: String?, loadedLabel: String?, selectedLabel: String, retry: () -> Unit) {
    if (loadedLabel != null && loadedLabel != selectedLabel) Text("Exibindo $loadedLabel. ${if (loading) "Carregando" else "Não foi possível carregar"} $selectedLabel.")
    if (error != null) AppDataErrorBanner(dataRequestErrorMessage(error, loadedLabel != null), retry, loading)
    if (loading && loadedLabel == null) AppLoadingIndicator()
}
@Composable
private fun PrivateFeaturePlaceholder() { Card(Modifier.fillMaxWidth()) { Text("Valores ocultos. Ative a visualização para consultar os relatórios.", Modifier.padding(24.dp)) } }
@Composable
private fun FinancialPanel(title: String, values: List<Pair<String, Double>>) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        values.forEach { (label, value) -> Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); FinancialValue(formatCurrency(value), false) }
    } }
}
internal fun periodLabel(month: Int, year: Int): String = if (month in 1..12) Period(month, year).label else "Período não informado"
@Composable
private fun IncomeExpenseChart(points: List<ChartReportResponse>) {
    val incomeColor = MaterialTheme.colorScheme.primary; val expenseColor = MaterialTheme.colorScheme.error
    Canvas(Modifier.fillMaxWidth().height(180.dp).clearAndSetSemantics {}) {
        if (points.isEmpty()) return@Canvas
        val values = points.flatMap { listOf(it.income, it.expense) }.filter { it.isFinite() }
        val upper = max(1.0, values.maxOrNull() ?: 1.0); val lower = minOf(0.0, values.minOrNull() ?: 0.0)
        fun y(value: Double): Float = ((upper - value) / (upper - lower) * size.height).toFloat()
        val baseline = y(0.0); val step = size.width / points.size
        points.forEachIndexed { index, p ->
            listOf(p.income to incomeColor, p.expense to expenseColor).forEachIndexed { column, (value, color) ->
                if (value.isFinite()) drawRect(color, Offset(index * step + step * (.12f + .4f * column), minOf(y(value), baseline)),
                    Size(step * .32f, kotlin.math.abs(y(value) - baseline)))
            }
        }
        drawLine(incomeColor.copy(alpha = .3f), Offset(0f, baseline), Offset(size.width, baseline))
    }
}
