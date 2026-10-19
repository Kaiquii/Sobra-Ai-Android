package br.com.sobraai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.components.AppDataErrorBanner

@Composable
internal fun FinanceWorkspaceScreen(workspace: FinanceWorkspace, hidden: Boolean, onTogglePrivacy: () -> Unit, onLogout: () -> Unit) {
    val route by workspace.route.collectAsState()
    val income by workspace.income.collectAsState()
    when (route) {
        FinanceRoute.Home -> HomeScreen(workspace.home, hidden, onTogglePrivacy, onLogout, workspace)
        FinanceRoute.Expenses -> ExpensesScreen(workspace, hidden, onTogglePrivacy)
        FinanceRoute.Incomes -> IncomesScreen(workspace, hidden, onTogglePrivacy)
        FinanceRoute.ExpenseEditor -> workspace.expenseEditor?.let { ExpenseEditorScreen(it) { workspace.navigate(FinanceRoute.Expenses) } }
        FinanceRoute.Reports -> workspace.features?.let { ReportsScreen(workspace, it.reports, hidden, onTogglePrivacy) }
        FinanceRoute.Commitments -> workspace.features?.let { CommitmentsScreen(workspace, it.reports, hidden, onTogglePrivacy) }
        FinanceRoute.Assistant -> workspace.features?.let { AssistantScreen(workspace, it.assistant, hidden, onTogglePrivacy) }
        FinanceRoute.Profile -> workspace.features?.let { ProfileScreen(workspace, it.profile, onLogout) }
        FinanceRoute.Categories -> workspace.features?.let { CategoriesScreen(workspace, it.profile) }
        FinanceRoute.Help -> workspace.features?.let { HelpScreen(workspace, it.native) }
    }
    income?.let { IncomeEditorDialog(it, workspace::dismissIncome) }
}

@Composable
internal fun ExpenseEditorScreen(editor: ExpenseEditor, onBack: () -> Unit) {
    val state by editor.state.collectAsState()
    val mutation by editor.mutation.state.collectAsState()
    val draft = state.draft
    val enabled = !mutation.busy && !state.loading && !state.categoryBusy
    var discard by remember { mutableStateOf(false) }
    var confirmFuture by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader(if (state.original != null) "Editar Despesa" else "Nova Despesa", { discard = true }, !mutation.busy && !state.categoryBusy) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.loading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
            state.loadError?.let { AppDataErrorBanner(it, editor::load, state.loading) }
            if (!state.loading && state.loadError == null) {
                FormInput("Valor", draft.amount, { value -> editor.update { it.copy(amount = value) } }, enabled, KeyboardType.Decimal)
                FormInput("Descrição", draft.description, { value -> editor.update { it.copy(description = value) } }, enabled)
                ChoiceField("Categoria", state.categories.find { it.id == draft.categoryId }?.name.orEmpty(), state.categories.map { it.name }, enabled) { index ->
                    editor.update { it.copy(categoryId = state.categories[index].id) }
                }
                TextButton(onClick = { category = true }, enabled = enabled) { Text("Criar categoria") }
                FormCheck("Dividir pagamento entre origens", draft.split, { checked -> editor.update {
                    it.copy(split = checked, splits = it.splits.ifEmpty { listOf(SplitDraft(it.source, it.amount)) })
                } }, enabled)
                if (draft.split) {
                    draft.splits.forEachIndexed { index, split ->
                        ChoiceField("Origem ${index + 1}", sourceLabel(split.source), PAYMENT_SOURCES, enabled) { selected ->
                            editor.update { it.copy(splits = it.splits.mapIndexed { i, value -> if (i == index) value.copy(source = PAYMENT_SOURCES[selected]) else value }) }
                        }
                        FormInput("Valor da origem ${index + 1}", split.amount, { amount -> editor.update {
                            it.copy(splits = it.splits.mapIndexed { i, value -> if (i == index) value.copy(amount = amount) else value })
                        } }, enabled, KeyboardType.Decimal)
                        if (draft.splits.size > 1) TextButton(onClick = { editor.update { it.copy(splits = it.splits.filterIndexed { i, _ -> i != index }) } }, enabled = enabled) { Text("Remover origem ${index + 1}") }
                    }
                    if (draft.splits.size < PAYMENT_SOURCES.size) TextButton(onClick = {
                        val next = PAYMENT_SOURCES.firstOrNull { source -> draft.splits.none { sourceKey(it.source) == sourceKey(source) } } ?: return@TextButton
                        editor.update { it.copy(splits = it.splits + SplitDraft(next, "")) }
                    }, enabled = enabled) { Text("Adicionar origem") }
                    FinancialValue("Distribuído: ${formatCurrency(draft.splits.sumOf { parseAmount(it.amount) ?: 0.0 })}", false)
                } else ChoiceField("Origem do pagamento", sourceLabel(draft.source), PAYMENT_SOURCES, enabled) { selected -> editor.update { it.copy(source = PAYMENT_SOURCES[selected]) } }
                DateField("Data", draft.date, { value -> editor.update { it.copy(date = value) } }, enabled)
                if (state.original == null) {
                    ChoiceField("Tipo", draft.type, EXPENSE_TYPES, enabled) { selected -> editor.update { it.copy(type = EXPENSE_TYPES[selected]) } }
                    if (draft.type == "Parcelada") FormInput("Número de parcelas", draft.installments, { value -> editor.update { it.copy(installments = value) } }, enabled, KeyboardType.Number)
                } else {
                    Text("Tipo: ${expenseType(state.original!!)}")
                    if (state.original!!.isRecurring) FormCheck("Atualizar esta e as próximas ocorrências", draft.updateFuture,
                        { value -> editor.update { it.copy(updateFuture = value) } }, enabled)
                }
                FormInput("Observações", draft.notes, { value -> editor.update { it.copy(notes = value.take(500)) } }, enabled,
                    singleLine = false, supporting = "${draft.notes.length}/500")
                MutationFeedback(mutation)
                Button(onClick = {
                    if (state.original?.isRecurring == true && draft.updateFuture && draft.validate(editing = true) == null) confirmFuture = true
                    else editor.save()
                }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Salvar despesa") }
            }
        }
    }
    if (discard) ConfirmDialog("Descartar alterações?", "As alterações que você não salvou serão perdidas.", "Descartar", { discard = false; onBack() }, { discard = false })
    if (confirmFuture) ConfirmDialog("Atualizar próximas ocorrências?", "Esta edição será aplicada à ocorrência atual e às próximas desta despesa.", "Atualizar", {
        confirmFuture = false; editor.save(true)
    }, { confirmFuture = false })
    if (category) AlertDialog(onDismissRequest = { if (!state.categoryBusy) category = false }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Criar categoria") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormInput("Nome", categoryName, { categoryName = it }, !state.categoryBusy)
                state.categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.categoryBusy) AppLoadingIndicator()
            }
        }, confirmButton = { TextButton(onClick = { editor.createCategory(categoryName) { category = false; categoryName = "" } }, enabled = !state.categoryBusy) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { category = false }, enabled = !state.categoryBusy) { Text("Cancelar") } })
}

@Composable
internal fun IncomeEditorDialog(editor: IncomeEditor, onDismiss: () -> Unit) {
    val draft by editor.state.collectAsState()
    val mutation by editor.mutation.state.collectAsState()
    var confirmation by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!mutation.busy) onDismiss() }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text("${if (draft.existing == null) "Criar" else "Editar"} ${sourceLabel(draft.source)}") }, text = {
            Column(Modifier.heightIn(max = 460.dp).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(draft.period.label)
                FormInput("Valor", draft.amount, { amount -> editor.update { it.copy(amount = amount) } }, !mutation.busy, KeyboardType.Decimal)
                if (draft.existing != null) FormCheck("Atualizar este e os próximos meses", draft.updateFuture,
                    { value -> editor.update { it.copy(updateFuture = value) } }, !mutation.busy)
                else if (sourceKey(draft.source) == "renda extra") FormCheck("Repetir nos próximos meses", draft.repeatFuture,
                    { value -> editor.update { it.copy(repeatFuture = value) } }, !mutation.busy)
                if (draft.existing != null) {
                    FormCheck("Ao excluir, remover esta e as próximas", draft.deleteFuture,
                        { value -> editor.update { it.copy(deleteFuture = value) } }, !mutation.busy)
                    TextButton(onClick = { confirmation = "delete" }, enabled = !mutation.busy) { Text("Excluir renda", color = MaterialTheme.colorScheme.error) }
                }
                MutationFeedback(mutation)
            }
        }, confirmButton = { TextButton(onClick = {
            if (parseAmount(draft.amount) != null && (draft.existing != null && draft.updateFuture || draft.existing == null && draft.repeatFuture)) confirmation = "future"
            else editor.save()
        }, enabled = !mutation.busy) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !mutation.busy) { Text("Cancelar") } })
    confirmation?.let { action -> ConfirmDialog(if (action == "delete") "Excluir renda?" else "Aplicar aos próximos meses?",
        if (action == "delete") {
            if (draft.deleteFuture) "Esta renda e suas próximas ocorrências serão excluídas." else "Esta ocorrência será excluída."
        } else "A alteração será aplicada também aos próximos meses.", if (action == "delete") "Excluir" else "Salvar", {
            confirmation = null
            if (action == "delete") editor.delete(true) else editor.save(true)
        }, { confirmation = null }) }
}

@Composable
internal fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background,
        title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
