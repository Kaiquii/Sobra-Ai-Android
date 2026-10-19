package br.com.sobraai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.components.*

@Composable
internal fun IncomesScreen(workspace: FinanceWorkspace, hidden: Boolean, onTogglePrivacy: () -> Unit) {
    val presenter = workspace.home
    val state by presenter.state.collectAsState()
    val snapshot = state.snapshot
    val enabled = !hidden && !state.loading && snapshot != null && snapshot.period == state.period
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Configurações de Renda", { workspace.navigate(FinanceRoute.Home) }) {
            if (state.loading && snapshot != null) AppLoadingIndicator(Modifier.padding(8.dp))
            IconButton(onClick = onTogglePrivacy) { Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (hidden) "Mostrar valores" else "Ocultar valores") }
        } }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Incomes) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PeriodSelector(state.period, presenter::selectPeriod) }
            if (snapshot != null && snapshot.period != state.period) item { Text("Exibindo dados de ${snapshot.period.label}.") }
            state.error?.let { error -> item { AppDataErrorBanner(dataRequestErrorMessage(error, snapshot != null), presenter::refresh, state.loading) } }
            if (state.loading && snapshot == null) item { AppLoadingIndicator() }
            item { Text("Adicionar renda", style = MaterialTheme.typography.titleMedium) }
            items(PAYMENT_SOURCES) { source ->
                val existing = snapshot?.incomes?.firstOrNull { sourceKey(it.source) == sourceKey(source) }
                OutlinedButton(onClick = { workspace.editIncome(source, existing, state.period) }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("${if (existing == null) "Adicionar" else "Editar"} $source") }
            }
            if (snapshot != null && snapshot.incomes.isEmpty()) item { Text("Nenhuma renda cadastrada neste mês.") }
            items(snapshot?.incomes.orEmpty(), key = { it.id }) { income ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sourceLabel(income.source))
                        Text(Period(income.month, income.year).label)
                        FinancialValue(formatCurrency(income.amount), hidden)
                        TextButton(onClick = { workspace.editIncome(income.source, income, Period(income.month, income.year)) }, enabled = enabled) {
                            Text("Editar ou excluir renda")
                        }
                    }
                }
            }
        }
    }
}
