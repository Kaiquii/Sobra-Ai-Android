package br.com.sobraai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun FinanceHeader(title: String, onBack: () -> Unit, enabled: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = enabled) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        actions()
    }
}

@Composable
internal fun FinanceBottomBar(workspace: FinanceWorkspace, route: FinanceRoute) {
    NavigationBar {
        NavigationBarItem(selected = route == FinanceRoute.Home, onClick = { workspace.navigate(FinanceRoute.Home) },
            icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
        NavigationBarItem(selected = route == FinanceRoute.Expenses, onClick = { workspace.navigate(FinanceRoute.Expenses) },
            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) }, label = { Text("Despesas") })
        NavigationBarItem(selected = route in listOf(FinanceRoute.Reports, FinanceRoute.Commitments), onClick = { workspace.navigate(FinanceRoute.Reports) },
            icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Relatórios") })
        NavigationBarItem(selected = route == FinanceRoute.Assistant, onClick = { workspace.navigate(FinanceRoute.Assistant) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) }, label = { Text("Assistente") })
        NavigationBarItem(selected = route in listOf(FinanceRoute.Profile, FinanceRoute.Incomes, FinanceRoute.Categories, FinanceRoute.Help), onClick = { workspace.navigate(FinanceRoute.Profile) },
            icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") })
    }
}

@Composable
internal fun PeriodSelector(period: Period, onSelect: (Period) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(period.shift(-1)) }) { Icon(Icons.Default.ChevronLeft, "Mês anterior") }
        Text(period.label, Modifier.weight(1f), fontWeight = FontWeight.Bold)
        IconButton(onClick = { onSelect(period.shift(1)) }) { Icon(Icons.Default.ChevronRight, "Próximo mês") }
    }
}

@Composable
internal fun FormInput(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, supporting: String? = null) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = singleLine,
        minLines = if (singleLine) 1 else 3, supportingText = supporting?.let { { Text(it) } })
}

@Composable
internal fun FormCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, null, enabled = enabled)
        Text(label, Modifier.weight(1f))
    }
}

@Composable
internal fun ChoiceField(label: String, selected: String, options: List<String>, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("$label: ${selected.ifBlank { "Selecione" }}", Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, "Selecionar $label")
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text(label) }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (options.isEmpty()) Text("Nenhuma opção disponível.")
                options.forEachIndexed { index, text -> TextButton(onClick = { onSelect(index); open = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) } }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("Fechar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean = true, before: FinancialDate? = null) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, label = { Text(label) }, placeholder = { Text("DD/MM/AAAA") },
        enabled = enabled, modifier = Modifier.fillMaxWidth(), singleLine = true,
        trailingIcon = { IconButton(onClick = { open = true }, enabled = enabled) { Icon(Icons.Default.CalendarToday, "Selecionar $label") } })
    if (open) {
        val date = FinancialDate.parseDisplay(value) ?: today()
        val selectable = remember(before) { object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = before == null || utcTimeMillis < before.utcMillis
        } }
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.utcMillis.takeIf { before == null || it < before.utcMillis },
            selectableDates = selectable)
        DatePickerDialog(onDismissRequest = { open = false }, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { onChange(FinancialDate.fromUtcMillis(it).display) }; open = false },
                enabled = picker.selectedDateMillis != null) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }) {
            DatePicker(picker, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
        }
    }
}

@Composable
internal fun MutationFeedback(state: MutationState) {
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (state.busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLoadingIndicator(); Text("Salvando...")
    }
}
