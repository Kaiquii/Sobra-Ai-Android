package com.example.appfinanceiro.feature.patrimonio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.*
import com.example.appfinanceiro.core.designsystem.components.*
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardBlue
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardBlueLight
import com.example.appfinanceiro.core.network.*
import kotlin.time.Instant

private data class PatrimonyEditor(val fgts: Boolean, val investment: Investment? = null,
    val amount: PatrimonyAmount? = null)
private data class PatrimonyDeletion(val investment: Investment? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrimonyScreen(onBack: () -> Unit, onSessionExpired: () -> Unit,
    model: PatrimonyViewModel = rememberPresentation("PatrimonyViewModel") { PatrimonyViewModel(it.patrimony) },
    categoriesModel: InvestmentCategoriesViewModel = rememberPresentation("InvestmentCategoriesViewModel") { InvestmentCategoriesViewModel(it.investmentCategories) }) {
    val services = LocalUiServices.current
    val token by services.session.token.collectAsState(initial = null)
    val state by model.state.collectAsState()
    val categoryState by categoriesModel.state.collectAsState()
    var showCategoryManager by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var handledCategoryMutation by rememberSaveable { mutableLongStateOf(categoryState.mutationRevision) }
    var editor by remember { mutableStateOf<PatrimonyEditor?>(null) }
    var deletion by remember { mutableStateOf<PatrimonyDeletion?>(null) }
    var handledMutation by rememberSaveable { mutableLongStateOf(state.mutationRevision) }
    val colors = MaterialTheme.colorScheme
    val actionsEnabled = state.data != null && state.loadError == null && !state.loading && !state.saving && !state.isSessionExpired

    LaunchedEffect(token) { token?.let { model.load(it); categoriesModel.load(it) } }
    LaunchedEffect(state.isSessionExpired, categoryState.isSessionExpired) {
        if (state.isSessionExpired || categoryState.isSessionExpired) onSessionExpired()
    }
    LaunchedEffect(state.successMessage, state.mutationRevision) {
        if (state.mutationRevision != handledMutation) {
            editor = null; deletion = null; handledMutation = state.mutationRevision
        }
        state.successMessage?.let {
            services.notify(it, AppNotificationType.Success)
            model.consumeSuccess()
        }
    }

    LaunchedEffect(state.reviewNotice) {
        state.reviewNotice?.let {
            editor = null
            deletion = null
            token?.let(categoriesModel::refreshAfterInvestmentChange)
            services.notify(it, AppNotificationType.Warning)
            model.consumeReviewNotice()
        }
    }

    LaunchedEffect(categoryState.mutationRevision) {
        if (handledCategoryMutation != categoryState.mutationRevision) {
            handledCategoryMutation = categoryState.mutationRevision
            showNewCategory = false
            token?.let(model::refreshAfterCategoryChange)
        }
    }
    LaunchedEffect(categoryState.successMessage) {
        categoryState.successMessage?.let {
            services.notify(it, AppNotificationType.Success)
            categoriesModel.consumeSuccess()
        }
    }
    LaunchedEffect(categoryState.reviewNotice) {
        categoryState.reviewNotice?.let {
            showNewCategory = false
            token?.let(model::refreshAfterCategoryChange)
            services.notify(it, AppNotificationType.Warning)
            categoriesModel.consumeReviewNotice()
        }
    }
    LaunchedEffect(categoryState.createdCategoryId, editor) {
        if (editor == null && categoryState.createdCategoryId != null) categoriesModel.consumeCreatedCategory()
    }

    Scaffold(containerColor = colors.background, topBar = {
        TopAppBar(title = { Text("Patrimônio", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            } }, actions = {
                if (state.loading && state.data != null) AppLoadingIndicator()
                FinancialPrivacyButton()
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.loadError?.let { error -> item(key = "patrimony_error") {
                AppDataErrorBanner(message = dataRequestErrorMessage(error, state.data != null, "seu patrimônio"),
                    isRetrying = state.loading, onRetry = { token?.let { model.load(it, retry = true) } })
            } }
            item(key = "patrimony_total") { PatrimonyTotals(state.data) }
            if (state.loading && state.data == null) {
                item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AppLoadingIndicator(size = 36.dp)
                } }
            }
            state.data?.let { data ->
                item(key = "fgts") {
                    val fgts = data.fgts
                    val openFgts = {
                        model.clearMutationError()
                        editor = PatrimonyEditor(true, amount = fgts?.amount)
                    }
                    if (fgts == null) {
                        PatrimonyEmptyCard("FGTS", "Informe seu saldo atual", Icons.Default.AccountBalance,
                            PrimaryBlue, "Informar", actionsEnabled, openFgts)
                    } else {
                        PatrimonyBalanceCard("FGTS", null, fgts.amount, fgts.updated_at,
                            Icons.Default.AccountBalance, PrimaryBlue, actionsEnabled, openFgts)
                    }
                }
                item(key = "investment_title") {
                    FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically) {
                        Text("Investimentos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        if (categoryState.supported == true) {
                            TextButton(onClick = { categoriesModel.clearMutationError(); showCategoryManager = true }) {
                                Text("Categorias")
                            }
                        }
                        FilledTonalButton(enabled = actionsEnabled, onClick = {
                            model.clearMutationError(); editor = PatrimonyEditor(fgts = false)
                        }, colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.14f), contentColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (data.investments.isEmpty()) item(key = "investments_empty") {
                    PatrimonyEmptyCard("Seu primeiro investimento", "Nome, categoria e saldo atual",
                        Icons.Default.TrendingUp, patrimonyInvestmentColor(), "Adicionar", actionsEnabled) {
                        model.clearMutationError(); editor = PatrimonyEditor(fgts = false)
                    }
                }
                items(data.investments, key = { "investment_" + it.id }) { investment ->
                    PatrimonyBalanceCard(investment.name, investment.category, investment.amount, investment.updated_at,
                        Icons.Default.TrendingUp, patrimonyInvestmentColor(), actionsEnabled, onEdit = {
                            model.clearMutationError(); editor = PatrimonyEditor(false, investment)
                        })
                }
            }
            item { Text("Saldos informados por você, separados do saldo do mês.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }

    editor?.let { editing ->
        PatrimonyEditorSheet(editing, state.saving, actionsEnabled, state.mutationError, categoryState,
            onCategoryRetry = { token?.let { categoriesModel.load(it, retry = true) } },
            onCreateCategory = { categoriesModel.clearMutationError(); showNewCategory = true },
            onManageCategories = { categoriesModel.clearMutationError(); showCategoryManager = true },
            onCreatedCategoryConsumed = categoriesModel::consumeCreatedCategory,
            onDismiss = { if (!state.saving) { editor = null; model.clearMutationError() } },
            onDelete = {
                editor = null
                model.clearMutationError()
                deletion = PatrimonyDeletion(editing.investment)
            },
            onSave = { name, category, amount, categoryId -> token?.let {
                if (editing.fgts) model.saveFgts(it, amount)
                else model.saveInvestment(it, editing.investment?.id, name, category, amount, categoryId)
            } })
    }
    if (showCategoryManager) {
        InvestmentCategoriesDialog(categoryState,
            onDismiss = { showCategoryManager = false },
            onRetry = { token?.let { categoriesModel.load(it, retry = true) } },
            onCreate = { name -> token?.let { categoriesModel.create(it, name) } },
            onRename = { category, name -> token?.let { categoriesModel.rename(it, category, name) } },
            onDelete = { category -> token?.let { categoriesModel.delete(it, category) } },
            onClearMutationError = categoriesModel::clearMutationError)
    }
    if (showNewCategory) {
        InvestmentCategoryNameDialog(category = null, saving = categoryState.saving,
            canSave = categoryState.supported == true && !categoryState.loading && categoryState.loadError == null,
            error = categoryState.mutationError,
            onDismiss = { showNewCategory = false; categoriesModel.clearMutationError() },
            onSave = { name -> token?.let { categoriesModel.create(it, name) } })
    }
    deletion?.let { target ->
        AlertDialog(onDismissRequest = { if (!state.saving) { deletion = null; model.clearMutationError() } },
            containerColor = colors.background, titleContentColor = colors.onBackground,
            textContentColor = colors.onBackground,
            title = { Text(if (target.investment == null) "Remover saldo do FGTS?" else "Excluir investimento?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (target.investment == null) "Isso remove apenas o saldo de FGTS informado no aplicativo."
                    else "Excluir o investimento “" + target.investment.name + "” do seu patrimônio?")
                state.mutationError?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
            } },
            confirmButton = { TextButton(enabled = !state.saving, onClick = { token?.let {
                if (target.investment == null) model.deleteFgts(it) else model.deleteInvestment(it, target.investment.id)
            } }) { if (state.saving) AppLoadingIndicator() else Text("Excluir", color = colors.error) } },
            dismissButton = { TextButton(enabled = !state.saving, onClick = { deletion = null; model.clearMutationError() }) {
                Text("Cancelar")
            } })
    }
}

@Composable
private fun PatrimonyTotals(data: PatrimonyResponse?) {
    val colors = MaterialTheme.colorScheme
    val base = if (colors.background.luminance() < 0.5f) SurfaceCardBlue else SurfaceCardBlueLight
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.Transparent) {
        Column(Modifier.background(Brush.linearGradient(listOf(base, lerp(base, PrimaryBlue, 0.18f))))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Meu patrimônio", style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface.copy(alpha = 0.78f))
                    FinancialValue(data?.total_patrimony?.formatted() ?: "—", color = colors.onSurface,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                PatrimonyIcon(Icons.Default.AccountBalanceWallet, PrimaryBlue, size = 40)
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PatrimonyTotalPart("FGTS", data?.total_fgts, Modifier.weight(1f))
                PatrimonyTotalPart("Investimentos", data?.total_investments, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PatrimonyTotalPart(label: String, amount: PatrimonyAmount?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FinancialValue(amount?.formatted() ?: "—", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun patrimonyInvestmentColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) GreenPositive else Color(0xFF287D3C)

@Composable
private fun PatrimonyIcon(icon: ImageVector, color: Color, size: Int = 32) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size((size * 0.6f).dp))
        }
    }
}

@Composable
private fun PatrimonyEmptyCard(title: String, subtitle: String, icon: ImageVector, accent: Color,
    action: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), color = colors.surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PatrimonyIcon(icon, accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(action, color = accent, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 42.dp))
        }
    }
}

@Composable
private fun PatrimonyBalanceCard(title: String, category: String?, amount: PatrimonyAmount, updatedAt: String,
    icon: ImageVector, accent: Color, enabled: Boolean, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onEdit, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), color = colors.surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PatrimonyIcon(icon, accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    category?.let { Text(it, color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Icon(Icons.Default.ChevronRight, "Atualizar saldo", tint = accent, modifier = Modifier.size(20.dp))
            }
            FinancialValue(amount.formatted(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text("Atualizado em " + patrimonyUpdatedAt(updatedAt), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text("Atualizar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = accent)
            }
        }
    }
}

internal fun patrimonyUpdatedAt(value: String): String = try {
    val instant = Instant.parse(value)
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(instant.toEpochMilliseconds())
} catch (_: IllegalArgumentException) { "data indisponível" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatrimonyEditorSheet(editor: PatrimonyEditor, saving: Boolean, canSave: Boolean, error: String?,
    categoryState: InvestmentCategoriesUiState, onCategoryRetry: () -> Unit,
    onCreateCategory: () -> Unit, onManageCategories: () -> Unit, onCreatedCategoryConsumed: () -> Unit,
    onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (String, String, String, Int?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(editor.investment?.name.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(editor.investment?.category.orEmpty()) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(editor.investment?.category_id) }
    var categoryExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!editor.fgts && categoryState.supported == false) onCategoryRetry()
    }
    val selectedCategory = categoryState.categories.firstOrNull { it.id == selectedCategoryId }
    val categoryReady = editor.fgts ||
        (categoryState.supported == false && !categoryState.loading && categoryState.loadError == null) ||
        (categoryState.supported == true && !categoryState.loading && categoryState.loadError == null && selectedCategory != null)
    LaunchedEffect(categoryState.createdCategoryId) {
        categoryState.createdCategoryId?.let { selectedCategoryId = it; onCreatedCategoryConsumed() }
    }
    LaunchedEffect(categoryState.categories, categoryState.supported) {
        if (selectedCategoryId == null && categoryState.supported == true) {
            selectedCategoryId = categoryState.categories.firstOrNull { it.name.equals(category, ignoreCase = true) }?.id
        }
    }
    var amount by rememberSaveable { mutableStateOf((editor.investment?.amount ?: editor.amount)?.input().orEmpty()) }
    val colors = MaterialTheme.colorScheme
    val existing = editor.investment != null || editor.amount != null
    val title = if (editor.fgts) "Saldo do FGTS" else if (!existing) "Novo investimento" else "Atualizar investimento"
    AppModalBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true,
        dismissEnabled = !saving,
        containerColor = colors.background, contentColor = colors.onBackground) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PatrimonyIcon(if (editor.fgts) Icons.Default.AccountBalance else Icons.Default.TrendingUp,
                    if (editor.fgts) PrimaryBlue else patrimonyInvestmentColor(), size = 40)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(enabled = !saving, onClick = onDismiss) { Icon(Icons.Default.Close, "Fechar") }
            }
            if (!editor.fgts) {
                PatrimonyField("Nome do investimento", name, { name = it }, !saving, placeholder = "Ex.: Minha reserva")
                categoryState.loadError?.let {
                    AppDataErrorBanner(message = dataRequestErrorMessage(it, categoryState.categories.isNotEmpty(), "as categorias"),
                        isRetrying = categoryState.loading, onRetry = onCategoryRetry)
                }
                when (categoryState.supported) {
                    false -> {
                        PatrimonyField("Categoria", category, { category = it }, !saving, placeholder = "Ex.: CDB, Tesouro ou Fundos")
                        Text("O catálogo de categorias ainda não está disponível no servidor. Você pode informar a categoria por enquanto.",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    else -> {
                        if (categoryState.loading) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppLoadingIndicator()
                                Text("Carregando categorias…", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                        AppSelectField(label = "Categoria", value = selectedCategory?.name.orEmpty(),
                            options = categoryState.categories.map { AppSelectOption<Int?>(it.id, it.name) },
                            selected = selectedCategoryId, expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }, onSelect = { selectedCategoryId = it },
                            enabled = !saving && !categoryState.loading && categoryState.loadError == null,
                            placeholder = "Selecione a categoria",
                            trailingAction = {
                                TextButton(onClick = onCreateCategory,
                                    enabled = !saving && !categoryState.saving && categoryState.supported == true && categoryState.loadError == null) {
                                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp)); Text("Nova categoria")
                                }
                            })
                        if (categoryState.supported == true) {
                            TextButton(onClick = onManageCategories, enabled = !saving && !categoryState.saving) {
                                Text("Gerenciar categorias")
                            }
                        }
                    }
                }
            }
            PatrimonyField("Saldo atual", amount, { amount = it }, !saving, decimal = true, placeholder = "0,00")
            Text(if (existing) "Informe o valor total que você tem hoje. Ele substitui o saldo anterior."
                else "Informe o valor total que você tem hoje. Você pode atualizar depois.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Button(enabled = !saving && canSave && categoryReady && !categoryState.saving, onClick = {
                onSave(name, category, amount, selectedCategoryId.takeIf { !editor.fgts && categoryState.supported == true })
            },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White)) {
                if (saving) {
                    AppLoadingIndicator()
                    Spacer(Modifier.width(8.dp))
                    Text("Salvando…")
                } else Text(if (editor.fgts) "Salvar saldo do FGTS" else if (existing) "Salvar alterações" else "Adicionar investimento",
                    fontWeight = FontWeight.Bold)
            }
            if (existing) {
                TextButton(enabled = !saving && canSave, onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp), tint = colors.error)
                    Spacer(Modifier.width(6.dp))
                    Text(if (editor.fgts) "Remover saldo do FGTS" else "Excluir investimento", color = colors.error)
                }
            }
        }
    }
}

@Composable
private fun PatrimonyField(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean,
    decimal: Boolean = false, placeholder: String) {
    val focus = LocalFocusManager.current
    OutlinedTextField(value = value, onValueChange = onChange, enabled = enabled, singleLine = true,
        modifier = Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) },
        prefix = if (decimal) ({ Text("R$ ") }) else null, shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text,
            imeAction = if (decimal) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) },
            onDone = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, cursorColor = PrimaryBlue))
}
