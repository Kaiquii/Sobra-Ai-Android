package br.com.sobraai.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.components.*
import com.example.appfinanceiro.core.network.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExpensesScreen(workspace: FinanceWorkspace, hidden: Boolean, onTogglePrivacy: () -> Unit) {
    val presenter = workspace.expenses
    val state by presenter.state.collectAsState()
    var detail by remember { mutableStateOf<Expense?>(null) }
    var filters by remember { mutableStateOf<ExpenseFilters?>(null) }
    val snapshot = state.snapshot
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Despesas Mensais", { workspace.navigate(FinanceRoute.Home) }) {
            if (state.loading && snapshot != null) AppLoadingIndicator(Modifier.padding(8.dp))
            IconButton(onClick = onTogglePrivacy) { Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (hidden) "Mostrar valores" else "Ocultar valores") }
            IconButton(onClick = presenter::refresh, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Atualizar despesas") }
        } }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Expenses) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PeriodSelector(state.period, presenter::selectPeriod) }
            if (snapshot != null && snapshot.period != state.period) item { Text("Exibindo ${snapshot.period.label}. ${if (state.loading) "Carregando" else "Não foi possível carregar"} ${state.period.label}.") }
            state.error?.let { error -> item { AppDataErrorBanner(dataRequestErrorMessage(error, snapshot != null), presenter::refresh, state.loading) } }
            item { FormInput("Buscar por descrição ou valor", state.filters.search, { presenter.filter(state.filters.copy(search = it)) }) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todas", "Parceladas", "Únicas", "Fixas").forEach { type ->
                        val count = state.filters.copy(type = type).apply(snapshot?.scheduled.orEmpty()).size
                        FilterChip(state.filters.type == type, { presenter.filter(state.filters.copy(type = type)) }, label = { Text("$type ($count)") })
                    }
                }
            }
            item {
                OutlinedButton(onClick = { filters = state.filters }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Filtrar por categoria, origem e status")
                }
            }
            item {
                Text("Total no período efetivo${snapshot?.let { " (${it.period.label})" }.orEmpty()}", fontWeight = FontWeight.Bold)
                if (snapshot != null) FinancialValue(formatCurrency(state.total), hidden) else Text("Não carregado")
            }
            if (state.loading && snapshot == null) item { AppLoadingIndicator() }
            if (snapshot != null && state.scheduled.isEmpty() && state.incoming.isEmpty()) item { Text("Nenhuma despesa encontrada para este período e filtros.") }
            items(state.scheduled, key = { "scheduled-${it.id}" }) { expense ->
                ExpenseCard(expense, snapshot?.categories?.get(expense.category_id) ?: "Outros", hidden) { detail = expense }
            }
            if (state.incoming.isNotEmpty()) item {
                Text("Despesas de outros meses adiantadas para este período", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Estes valores já estão no total do período efetivo e não serão descontados novamente no mês previsto.")
            }
            items(state.incoming, key = { "incoming-${it.id}" }) { expense ->
                ExpenseCard(expense, snapshot?.categories?.get(expense.category_id) ?: "Outros", hidden) { detail = expense }
            }
            item { Button(onClick = { workspace.editExpense() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Adicionar despesa") } }
        }
    }
    detail?.let { expense ->
        // Refreshing after a successful action closes the dialog; failures keep it available.
        ExpenseDetails(expense, snapshot?.categories?.get(expense.category_id) ?: "Outros", hidden, workspace) { detail = null }
    }
    filters?.let { draft ->
        val options = snapshot?.categoryOptions.orEmpty()
        AlertDialog(onDismissRequest = { filters = null }, containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Filtrar despesas") }, text = {
                Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val names = listOf("Todas") + options.values
                    ChoiceField("Categoria", draft.category?.let { snapshot?.categories?.get(it) } ?: "Todas", names) { index ->
                        filters = draft.copy(category = if (index == 0) null else options.keys.toList()[index - 1])
                    }
                    if (draft.category != null && draft.category !in options) Text("A categoria selecionada não possui despesas neste período. Você pode limpar ou trocar o filtro.")
                    ChoiceField("Origem", draft.source?.let(::sourceLabel) ?: "Todas", listOf("Todas") + PAYMENT_SOURCES) { index ->
                        filters = draft.copy(source = if (index == 0) null else PAYMENT_SOURCES[index - 1])
                    }
                    ChoiceField("Status", when (draft.status) { "paid" -> "Pagas"; "pending" -> "Pendentes"; else -> "Todas" }, listOf("Todas", "Pagas", "Pendentes")) { index ->
                        filters = draft.copy(status = listOf(null, "paid", "pending")[index])
                    }
                    TextButton(onClick = { filters = draft.copy(category = null, source = null, status = null) }) { Text("Limpar filtros") }
                }
            }, confirmButton = { TextButton(onClick = { presenter.filter(draft); filters = null }) { Text("Aplicar") } },
            dismissButton = { TextButton(onClick = { filters = null }) { Text("Cancelar") } })
    }
}

@Composable
internal fun ExpenseCard(expense: Expense, category: String, hidden: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(expense.displayDescription, fontWeight = FontWeight.Bold)
            Text(category, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${expense.paymentCardSourceLabel()} • ${expenseType(expense)}")
            Text("Prevista: ${formatDate(expense.date)} • ${if (expense.is_paid) "Paga" else "Pendente"}")
            if (expense.isAdvanced) Text("Adiantada${expense.advancedAt?.let { " em ${formatDate(it)}" }.orEmpty()}")
            FinancialValue("- ${formatCurrency(expense.amount)}", hidden)
        }
    }
}

@Composable
internal fun ExpenseDetails(expense: Expense, category: String, hidden: Boolean, workspace: FinanceWorkspace, onDismiss: () -> Unit) {
    var action by remember(expense.id) { mutableStateOf<String?>(null) }
    val runner = remember(expense.id) { workspace.newAction() }
    val mutation by runner.state.collectAsState()
    var future by remember(expense.id) { mutableStateOf(false) }
    var advanceDate by remember(expense.id) { mutableStateOf(FinancialDate.parseApi(expense.advancedAt.orEmpty())?.display ?: today().display) }
    LaunchedEffect(mutation.completed) { if (mutation.completed) onDismiss() }
    AlertDialog(onDismissRequest = { if (!mutation.busy) onDismiss() }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text(expense.displayDescription) }, text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category); Text("${expenseType(expense)} • ${formatDate(expense.date)}")
                Text(if (expense.is_paid) "Paga" else "Pendente")
                expense.paid_at?.let { Text("Paga em ${formatDate(it)}") }
                if (expense.isAdvanced) Text("Adiantada${expense.advancedAt?.let { " em ${formatDate(it)}" }.orEmpty()}")
                FinancialValue(formatCurrency(expense.amount), hidden)
                Text(expense.paymentSourceLabel())
                expense.payment_splits.forEach { split -> Text(split.payment_source); FinancialValue(formatCurrency(split.amount), hidden) }
                expense.notes?.takeIf { it.isNotBlank() }?.let { Text("Observações: $it") }
                MutationFeedback(mutation)
                TextButton(onClick = { workspace.editExpense(expense.id) }, enabled = !mutation.busy && !hidden) { Text("Editar despesa") }
                TextButton(onClick = { action = "paid" }, enabled = !mutation.busy) { Text(if (expense.is_paid) "Marcar como pendente" else "Marcar como paga") }
                TextButton(onClick = { action = "advance" }, enabled = !mutation.busy && !hidden) { Text(if (expense.isAdvanced) "Alterar adiantamento" else "Adiantar despesa") }
                if (expense.isAdvanced) TextButton(onClick = { action = "removeAdvance" }, enabled = !mutation.busy) { Text("Remover adiantamento") }
                TextButton(onClick = { action = "delete" }, enabled = !mutation.busy) { Text("Excluir despesa", color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = onDismiss, enabled = !mutation.busy) { Text("Fechar") } })
    if (action == "delete") AlertDialog(onDismissRequest = { action = null }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Excluir despesa?") }, text = {
            Column {
                Text(expense.displayDescription)
                when {
                    expense.type.equals("Fixa", true) -> Text("Esta exclusão removerá esta despesa no mês atual e também nos próximos meses.")
                    expense.type.equals("Parcelada", true) -> FormCheck("Excluir esta e as próximas parcelas", future, { future = it })
                    else -> Text("Esta ocorrência será excluída.")
                }
            }
        }, confirmButton = { TextButton(onClick = { action = null; workspace.deleteExpense(expense, future, runner) }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } })
    if (action == "paid") ConfirmDialog(if (expense.is_paid) "Marcar como pendente?" else "Marcar como paga?",
        "O status de pagamento desta ocorrência será alterado.", "Confirmar", { action = null; workspace.setPaid(expense, !expense.is_paid, runner) }, { action = null })
    if (action == "removeAdvance") ConfirmDialog("Remover adiantamento?", "O valor voltará ao cálculo do período previsto. Esta ação não altera o status Paga.",
        "Remover", { action = null; workspace.advance(expense, null, runner) }, { action = null })
    if (action == "advance") {
        val selected = FinancialDate.parseDisplay(advanceDate)
        val scheduled = FinancialDate.parseApi(expense.date)
        AlertDialog(onDismissRequest = { action = null }, containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Adiantar despesa") }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Prevista para ${formatDate(expense.date)}. Escolha uma data anterior à prevista, inclusive uma data futura.")
                    DateField("Data do adiantamento", advanceDate, { advanceDate = it }, before = scheduled)
                    Text("O valor entrará no período escolhido e continuará visível no mês previsto. Esta ação não altera o status Paga.")
                    if (selected == null || scheduled == null || selected >= scheduled) Text("Escolha uma data válida anterior à prevista.", color = MaterialTheme.colorScheme.error)
                }
            }, confirmButton = { TextButton(onClick = { action = null; workspace.advance(expense, selected, runner) },
                enabled = selected != null && scheduled != null && selected < scheduled) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } })
    }
}
