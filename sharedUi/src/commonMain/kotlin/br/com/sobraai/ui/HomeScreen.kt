package br.com.sobraai.ui

import br.com.sobraai.ui.components.AppDataErrorBanner
import br.com.sobraai.ui.components.dataRequestErrorMessage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.network.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreen(presenter: HomePresenter, hidden: Boolean, onTogglePrivacy: () -> Unit, onLogout: () -> Unit, workspace: FinanceWorkspace) {
    val state by presenter.state.collectAsState()
    var exit by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<Expense?>(null) }
    val snapshot = state.snapshot
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { exit = true }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Sair da conta") }
                    Text("Visão Mensal", Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    if (state.loading && snapshot != null) AppLoadingIndicator(Modifier.padding(8.dp))
                    IconButton(onClick = onTogglePrivacy) {
                        Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            if (hidden) "Mostrar valores" else "Ocultar valores")
                    }
                    IconButton(onClick = presenter::refresh, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Atualizar dados") }
            }
        }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Home) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { presenter.selectPeriod(state.period.shift(-1)) }) { Icon(Icons.Default.ChevronLeft, "Mês anterior") }
                    Text(state.period.label, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { presenter.selectPeriod(state.period.shift(1)) }) { Icon(Icons.Default.ChevronRight, "Próximo mês") }
                }
            }
            if (snapshot != null && snapshot.period != state.period) item {
                Text("Exibindo dados de ${snapshot.period.label}. ${if (state.loading) "Carregando" else "Não foi possível carregar"} ${state.period.label}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.error?.let { error -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppDataErrorBanner(dataRequestErrorMessage(error, snapshot != null), presenter::refresh, state.loading)
                    if (snapshot != null) Text("Dados de ${snapshot.period.label}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } }
            item { Text("Resumo Financeiro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (snapshot == null && state.loading) item {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
            }
            item {
                val summary = snapshot?.summary
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyCard("Saldo disponível", summary?.total_geral_disponivel, hidden)
                    IncomeSummaryCard("Salário", summary?.salario, summary?.restante_salario, hidden, state, workspace)
                    IncomeSummaryCard("Adiantamento", summary?.adiantamento, summary?.restante_adiantamento, hidden, state, workspace)
                    IncomeSummaryCard("Renda Extra", summary?.renda_extra_amt, summary?.restante_renda_extra, hidden, state, workspace)
                    MoneyCard("Total de rendas", summary?.total_income, hidden)
                    MoneyCard("Total de despesas", summary?.total_expense, hidden)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Despesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.category != null, onClick = { filter = "Categoria" },
                            label = { Text(state.category?.let { snapshot?.categories?.get(it) } ?: "Categoria") })
                        FilterChip(selected = state.source != null, onClick = { filter = "Origem" }, label = { Text(if (state.source == "Salario") "Salário" else state.source ?: "Origem") })
                        FilterChip(selected = state.status != null, onClick = { filter = "Status" },
                            label = { Text(when (state.status) { "paid" -> "Pagas"; "pending" -> "Pendentes"; else -> "Status" }) })
                    }
                }
            }
            if (snapshot != null && state.filteredExpenses.isEmpty()) item { Text("Nenhuma despesa encontrada para este período e filtros.") }
            items(state.filteredExpenses, key = { it.id }) { expense ->
                Card(Modifier.fillMaxWidth().clickable { detail = expense }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(expense.displayDescription, fontWeight = FontWeight.Bold)
                        Text(snapshot?.categories?.get(expense.category_id) ?: "Outros", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${expense.paymentCardSourceLabel()} • ${expenseType(expense)}")
                        Text("${formatDate(expense.date)} • ${if (expense.is_paid) "Paga" else "Pendente"}")
                        if (expense.isAdvanced) Text("Adiantada${expense.advancedAt?.let { " em ${formatDate(it)}" }.orEmpty()}")
                        FinancialValue("- ${formatCurrency(expense.amount)}", hidden)
                    }
                }
            }
            item { Button(onClick = { workspace.editExpense() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Adicionar despesa") } }
        }
    }
    if (exit) AlertDialog(onDismissRequest = { exit = false },
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Sair da conta?") }, text = { Text("Você precisará entrar novamente para acessar seus dados.") },
        confirmButton = { TextButton(onClick = { exit = false; onLogout() }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { exit = false }) { Text("Cancelar") } })
    filter?.let { kind ->
        val options: List<Pair<String, String?>> = when (kind) {
            "Categoria" -> listOf("Todas" to null) + snapshot?.categories.orEmpty().map { it.value to it.key.toString() }
            "Origem" -> listOf("Todas" to null, "Salário" to "Salario", "Adiantamento" to "Adiantamento", "Renda Extra" to "Renda Extra")
            else -> listOf("Todas" to null, "Pagas" to "paid", "Pendentes" to "pending")
        }
        AlertDialog(onDismissRequest = { filter = null }, containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Filtrar por $kind") }, text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    options.forEach { (label, value) ->
                        TextButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            presenter.filter(if (kind == "Categoria") value?.toInt() else state.category,
                                if (kind == "Origem") value else state.source, if (kind == "Status") value else state.status)
                            filter = null
                        }) { Text(label) }
                    }
                }
            }, confirmButton = { TextButton(onClick = { filter = null }) { Text("Fechar") } })
    }
    detail?.let { expense ->
        ExpenseDetails(expense, snapshot?.categories?.get(expense.category_id) ?: "Outros", hidden, workspace) { detail = null }
    }
}

internal fun expenseType(expense: Expense): String = if (expense.type.equals("Parcelada", true) && expense.installments != null && expense.current_installment != null)
    "Parc. ${expense.current_installment}/${expense.installments}" else expense.type

@Composable
private fun IncomeSummaryCard(source: String, value: Double?, remaining: Double?, hidden: Boolean, state: HomeState, workspace: FinanceWorkspace) {
    Column {
        MoneyCard(source, value, hidden, "Restante", remaining)
        val snapshot = state.snapshot
        val enabled = !hidden && !state.loading && snapshot != null && snapshot.period == state.period
        val existing = snapshot?.incomes?.firstOrNull { sourceKey(it.source) == sourceKey(source) }
        TextButton(onClick = { workspace.editIncome(source, existing, state.period) }, enabled = enabled) {
            Text(if (existing == null) "Criar $source" else "Editar $source")
        }
    }
}

@Composable
private fun MoneyCard(title: String, value: Double?, hidden: Boolean, secondary: String? = null, secondaryValue: Double? = null) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FinancialValue(value?.let(::formatCurrency) ?: "Não carregado", hidden)
            secondary?.let { Text(it, style = MaterialTheme.typography.labelMedium)
                FinancialValue(secondaryValue?.let(::formatCurrency) ?: "Não carregado", hidden) }
        }
    }
}

@Composable
internal fun FinancialValue(value: String, hidden: Boolean) {
    Box {
        Text(value, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(if (hidden) 0f else 1f)
            .clearAndSetSemantics { if (!hidden) text = AnnotatedString(value) })
        if (hidden) Box(Modifier.matchParentSize().padding(vertical = 5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .18f), RoundedCornerShape(4.dp)))
    }
}
