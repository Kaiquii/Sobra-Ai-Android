package com.example.appfinanceiro.feature.despesas.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

@Composable
fun ExpenseFilterButton(active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics {
            contentDescription = "Filtrar despesas"
            stateDescription = if (active) "Filtros ativos" else "Sem filtros ativos"
        },
        shape = RoundedCornerShape(12.dp),
        color = if (active) colors.primaryContainer else colors.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(24.dp)) {
                listOf(0.8f, 0.55f, 0.3f).forEachIndexed { index, fraction ->
                    val inset = size.width * (1f - fraction) / 2f
                    val y = size.height * (0.25f + index * 0.25f)
                    drawLine(tint, Offset(inset, y), Offset(size.width - inset, y),
                        strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFiltersDialog(
    categories: Map<Int, String>,
    categoryId: Int?,
    paymentSource: String?,
    paymentStatus: String?,
    onDismiss: () -> Unit,
    onApply: (Int?, String?, String?) -> Unit,
    categoryOptions: Map<Int, String> = categories
) {
    // Dismissal removes this draft; only Apply publishes it to the screen.
    var draftCategory by rememberSaveable { mutableStateOf(categoryId) }
    var draftSource by rememberSaveable { mutableStateOf(paymentSource) }
    var draftStatus by rememberSaveable { mutableStateOf(paymentStatus) }
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrar despesas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar filtros")
                }
            }
            Text(
                text = "Escolha quais despesas você quer visualizar.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(20.dp))
            FilterSelector(
                label = "Categoria",
                value = draftCategory?.let { categories[it] ?: "Categoria indisponível" } ?: "Todas",
                options = listOf(null to "Todas") + categoryOptions.entries
                    .sortedBy { it.value.lowercase() }.map { it.key to it.value },
                selected = draftCategory,
                onSelect = { draftCategory = it }
            )
            if (draftCategory != null && draftCategory !in categoryOptions) {
                Text(
                    text = "Esta categoria não tem despesas neste mês. Selecione outra ou use Todas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            FilterSelector(
                label = "Origem do pagamento",
                value = when (draftSource) {
                    "Renda Extra" -> "Renda extra"
                    null -> "Todas"
                    else -> draftSource.orEmpty()
                },
                options = listOf(
                    null to "Todas",
                    "Salário" to "Salário",
                    "Adiantamento" to "Adiantamento",
                    "Renda Extra" to "Renda extra"
                ),
                selected = draftSource,
                onSelect = { draftSource = it }
            )
            Spacer(Modifier.height(20.dp))
            FilterSelector(
                label = "Status",
                value = when (draftStatus) {
                    "paid" -> "Pagas"
                    "pending" -> "Pendentes"
                    else -> "Todas"
                },
                options = listOf(null to "Todas", "paid" to "Pagas", "pending" to "Pendentes"),
                selected = draftStatus,
                onSelect = { draftStatus = it }
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onApply(draftCategory, draftSource, draftStatus) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Aplicar filtros", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = {
                    draftCategory = null
                    draftSource = null
                    draftStatus = null
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text("Limpar filtros", color = colors.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterSelector(
    label: String,
    value: String,
    options: List<Pair<T?, String>>,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Text(label, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = "$label: $value" },
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = colors.surface
        ) {
            options.forEach { (key, text) ->
                val isSelected = selected == key
                DropdownMenuItem(
                    text = {
                        Text(
                            text = text,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) colors.primary else colors.onSurface
                        )
                    },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary) }
                    } else null,
                    modifier = Modifier.semantics {
                        stateDescription = if (isSelected) "Selecionada" else "Não selecionada"
                    },
                    onClick = {
                        onSelect(key)
                        expanded = false
                    }
                )
            }
        }
    }
}
