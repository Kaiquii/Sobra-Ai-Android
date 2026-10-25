package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class AppSelectOption<T>(val value: T, val label: String)

/** Filter labels only; retain original identities, order and the caller's selection. */
fun <T> filterSelectOptions(options: List<AppSelectOption<T>>, query: String): List<AppSelectOption<T>> {
    fun normalize(text: String): String = text.lowercase()
        .replace(Regex("[áàâãä]"), "a").replace(Regex("[éèêë]"), "e")
        .replace(Regex("[íìîï]"), "i").replace(Regex("[óòôõö]"), "o")
        .replace(Regex("[úùûü]"), "u").replace('ç', 'c')
    val terms = normalize(query).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return if (terms.isEmpty()) options else options.filter { option ->
        val label = normalize(option.label)
        terms.all { it in label }
    }
}

/** One field and option list for both hosts. Selection remains owned by each consumer. */
@Composable
fun <T> AppSelectField(
    label: String,
    value: String,
    options: List<AppSelectOption<T>>,
    selected: T,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Selecione",
    trailingAction: (@Composable () -> Unit)? = null,
    anchor: (@Composable (open: () -> Unit) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    if (anchor != null) {
        anchor { if (enabled && options.isNotEmpty()) { focus.clearFocus(); onExpandedChange(true) } }
    } else Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = colors.onBackground, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            trailingAction?.invoke()
        }
        Surface(
            onClick = { focus.clearFocus(); onExpandedChange(true) },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().semantics {
                role = Role.Button
                contentDescription = "$label: ${value.ifBlank { placeholder }}"
            },
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, if (expanded) colors.primary else colors.outlineVariant.copy(alpha = 0.45f))
        ) {
            Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(value.ifBlank { placeholder }, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled && value.isNotBlank()) colors.onSurface else colors.onSurfaceVariant)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
    if (expanded && enabled) {
        AppSelectDialog(label, options, selected, { onExpandedChange(false) }) {
            onExpandedChange(false)
            onSelect(it)
        }
    }
}

/** Independent of the field's anchor so six short options need no cramped popup. */
@Composable
fun <T> AppSelectDialog(
    title: String,
    options: List<AppSelectOption<T>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    val filteredOptions = remember(options, query) { filterSelectOptions(options, query) }
    // Keep the viewport independent of the result count; typing must not resize/recenter the dialog.
    val listState = remember(query) { LazyListState() }
    val preferredHeight = (136 + options.size.coerceIn(1, 7) * 48).dp
    Dialog(onDismissRequest = onDismiss, properties = selectorDialogProperties()) {
        Box(Modifier.fillMaxSize()) {
            // Full-size host prevents the platform window from chasing the panel's size.
            Box(Modifier.matchParentSize().clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = { focus.clearFocus(); onDismiss() }
            ))
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().height(minOf(preferredHeight, maxHeight)),
                shape = RoundedCornerShape(20.dp), color = colors.background,
                contentColor = colors.onBackground,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f))) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fechar seleção") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.surface) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Search, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                modifier = Modifier.weight(1f).padding(vertical = 12.dp).semantics { contentDescription = "Buscar opções" },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                                cursorBrush = SolidColor(colors.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                                decorationBox = { input ->
                                    if (query.isEmpty()) Text("Buscar...", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                                    input()
                                }
                            )
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, "Limpar busca", modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), state = listState) {
                        if (filteredOptions.isEmpty()) item {
                            Text(if (options.isEmpty()) "Nenhuma opção disponível." else "Nenhuma opção encontrada.", Modifier.padding(12.dp), color = colors.onSurfaceVariant)
                        }
                        itemsIndexed(filteredOptions, key = { _, option -> options.indexOf(option) }) { _, option ->
                            val isSelected = option.value == selected
                            Surface(onClick = { onSelect(option.value) },
                                modifier = Modifier.fillMaxWidth().semantics {
                                    role = Role.RadioButton
                                    this.selected = isSelected
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.background) {
                                Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) colors.primary else colors.onBackground)
                                    if (isSelected) Icon(Icons.Default.Check, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
