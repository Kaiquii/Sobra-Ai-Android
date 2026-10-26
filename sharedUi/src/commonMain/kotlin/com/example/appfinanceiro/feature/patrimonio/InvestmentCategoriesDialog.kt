package com.example.appfinanceiro.feature.patrimonio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.core.designsystem.components.selectorDialogProperties
import com.example.appfinanceiro.core.network.InvestmentCategory
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

private data class CategoryNameEditor(val category: InvestmentCategory? = null)

/** The manager overlays the investment editor without replacing its unsaved draft. */
@Composable
fun InvestmentCategoriesDialog(
    state: InvestmentCategoriesUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (InvestmentCategory, String) -> Unit,
    onDelete: (InvestmentCategory) -> Unit,
    onClearMutationError: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    var nameEditor by remember { mutableStateOf<CategoryNameEditor?>(null) }
    var deletion by remember { mutableStateOf<InvestmentCategory?>(null) }
    var handledMutation by remember { mutableLongStateOf(state.mutationRevision) }
    val actionsEnabled = state.supported == true && !state.loading && !state.saving &&
        state.loadError == null && !state.isSessionExpired
    val dismiss = {
        if (!state.saving) {
            focus.clearFocus()
            onClearMutationError()
            onDismiss()
        }
    }

    LaunchedEffect(state.mutationRevision) {
        if (state.mutationRevision != handledMutation) {
            nameEditor = null
            deletion = null
            handledMutation = state.mutationRevision
        }
    }
    LaunchedEffect(state.reviewNotice) {
        if (state.reviewNotice != null) {
            // An uncertain write is reconciled by the ViewModel; never resubmit it here.
            nameEditor = null
            deletion = null
        }
    }

    Dialog(onDismissRequest = dismiss, properties = selectorDialogProperties()) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismiss
            ))
            BoxWithConstraints(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val panelHeight = minOf(600.dp, maxHeight)
                val compactHeight = maxHeight < 360.dp
                val spacing = if (compactHeight) 4.dp else 8.dp
                Surface(
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                        .heightIn(max = panelHeight),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.background,
                    contentColor = colors.onBackground,
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = if (compactHeight) 8.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Categorias de investimentos", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            IconButton(enabled = !state.saving, onClick = dismiss,
                                modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, "Fechar categorias")
                            }
                        }
                        if (!compactHeight) {
                            Text("Use uma categoria pronta ou crie a sua.",
                                Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant)
                        }
                        FilledTonalButton(
                            enabled = actionsEnabled,
                            onClick = { onClearMutationError(); nameEditor = CategoryNameEditor() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PrimaryBlue.copy(alpha = 0.14f), contentColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nova categoria", fontWeight = FontWeight.SemiBold)
                        }

                        // Status stays above the category viewport. Only unusually long messages
                        // scroll within this bounded region, keeping the close/create actions visible.
                        val hasMutationError = nameEditor == null && deletion == null && state.mutationError != null
                        if (state.loadError != null || state.supported == false || state.loading || hasMutationError) {
                            Column(Modifier.fillMaxWidth().heightIn(max = panelHeight / 3)
                                .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(spacing)) {
                                state.loadError?.let { error ->
                                    AppDataErrorBanner(
                                        message = dataRequestErrorMessage(error, state.categories.isNotEmpty(),
                                            "suas categorias de investimentos"),
                                        isRetrying = state.loading,
                                        onRetry = onRetry
                                    )
                                }
                                if (state.supported == false) {
                                    Text("O catálogo de categorias ainda não está disponível. Você pode continuar " +
                                        "informando a categoria no investimento.",
                                        Modifier.padding(4.dp), style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant)
                                }
                                if (state.loading && state.loadError == null) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = if (compactHeight) 4.dp else 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        AppLoadingIndicator()
                                        Text("Carregando categorias…", Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant)
                                    }
                                }
                                if (hasMutationError) state.mutationError?.let { CategoryMutationError(it) }
                            }
                        }

                        // Fixed context controls above; this is the only scrolling category area.
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            contentPadding = PaddingValues(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val personal = state.categories.filterNot { it.is_default }
                            val defaults = state.categories.filter { it.is_default }
                            if (personal.isNotEmpty()) {
                                item(key = "personal_title") { CategoryGroupTitle("Minhas categorias") }
                                items(personal, key = { "category_" + it.id }) { category ->
                                    InvestmentCategoryRow(category, actionsEnabled,
                                        onRename = {
                                            onClearMutationError()
                                            nameEditor = CategoryNameEditor(category)
                                        },
                                        onDelete = {
                                            onClearMutationError()
                                            deletion = category
                                        })
                                }
                            }
                            if (defaults.isNotEmpty()) {
                                item(key = "default_title") { CategoryGroupTitle("Categorias padrão") }
                                items(defaults, key = { "category_" + it.id }) { category ->
                                    InvestmentCategoryRow(category, actionsEnabled, onRename = {}, onDelete = {})
                                }
                            }
                            if (state.supported == true && !state.loading && state.loadError == null &&
                                state.categories.isEmpty()) item(key = "empty") {
                                Text("Nenhuma categoria disponível. Crie a primeira categoria para começar.",
                                    Modifier.padding(4.dp), style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    nameEditor?.let { editor ->
        InvestmentCategoryNameDialog(editor.category, state.saving, actionsEnabled, state.mutationError,
            onDismiss = { if (!state.saving) { nameEditor = null; onClearMutationError() } },
            onSave = { name ->
                if (editor.category == null) onCreate(name) else onRename(editor.category, name)
            })
    }
    deletion?.let { category ->
        AlertDialog(
            onDismissRequest = { if (!state.saving) { deletion = null; onClearMutationError() } },
            containerColor = colors.background,
            titleContentColor = colors.onBackground,
            textContentColor = colors.onBackground,
            title = { Text("Excluir categoria?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Excluir a categoria “${category.name}”?")
                    Text("Se estiver em uso, troque a categoria dos investimentos antes. " +
                        "Seus investimentos não serão excluídos.",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    state.mutationError?.let { CategoryMutationError(it) }
                }
            },
            confirmButton = {
                TextButton(enabled = !state.saving && actionsEnabled, onClick = { onDelete(category) }) {
                    if (state.saving) AppLoadingIndicator() else Text("Excluir", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !state.saving,
                    onClick = { deletion = null; onClearMutationError() }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CategoryGroupTitle(title: String) {
    Text(title, Modifier.padding(start = 4.dp, top = 8.dp),
        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InvestmentCategoryRow(
    category: InvestmentCategory,
    enabled: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = colors.surface) {
        Row(Modifier.heightIn(min = 56.dp).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface)
                Text(if (category.is_default) "Padrão · protegida" else "Pessoal",
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (category.is_default) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, "Categoria padrão protegida",
                        Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                }
            } else {
                IconButton(enabled = enabled, onClick = onRename, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, "Renomear categoria ${category.name}",
                        Modifier.size(20.dp), tint = if (enabled) PrimaryBlue else colors.onSurfaceVariant)
                }
                IconButton(enabled = enabled, onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Excluir categoria ${category.name}",
                        Modifier.size(20.dp), tint = if (enabled) colors.error else colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun InvestmentCategoryNameDialog(
    category: InvestmentCategory?,
    saving: Boolean,
    canSave: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    var name by rememberSaveable(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = colors.background,
        titleContentColor = colors.onBackground,
        textContentColor = colors.onBackground,
        title = { Text(if (category == null) "Nova categoria" else "Renomear categoria") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Nome da categoria") },
                    placeholder = { Text("Ex.: Minha reserva") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue, cursorColor = PrimaryBlue)
                )
                Text(if (category == null) "Esta categoria será pessoal e ficará disponível nos seus investimentos."
                    else "O novo nome também aparecerá nos investimentos que usam esta categoria.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                error?.let { CategoryMutationError(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving && canSave, onClick = { focus.clearFocus(); onSave(name) }) {
                if (saving) AppLoadingIndicator() else Text("Salvar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CategoryMutationError(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}
