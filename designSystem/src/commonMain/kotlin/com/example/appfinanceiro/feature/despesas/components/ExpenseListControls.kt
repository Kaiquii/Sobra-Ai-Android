package com.example.appfinanceiro.feature.despesas.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.components.FinancialValue
import com.example.appfinanceiro.core.designsystem.theme.*
import com.example.appfinanceiro.feature.home.components.MonthSelector

/** Shared visual header; values and filter decisions are supplied by the screen. */
@Composable
fun ExpenseListControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit,
    filtersActive: Boolean,
    onShowFilters: () -> Unit,
    currentMonthIndex: Int,
    currentYear: Int,
    count: Int?,
    formattedTotal: String?,
    totalLabel: String,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonthSelector(currentMonthIndex, currentYear, onPrevClick, onNextClick)
        ExpenseTotalSummary(formattedTotal, count, totalLabel)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ExpenseSearchField(searchQuery, onSearchQueryChange, Modifier.weight(1f))
            ExpenseFilterButton(filtersActive, onShowFilters)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Parceladas", "Únicas", "Fixas").forEach { filter ->
                val selected = selectedFilter == filter
                val interaction = remember { MutableInteractionSource() }
                val shape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier.heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onFilterSelect(filter) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.clip(shape).indication(interaction, ripple()),
                        shape = shape,
                        color = if (selected) PrimaryBlue else colors.surface,
                        border = if (selected) null else BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.08f))
                    ) {
                        Text(filter, Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = if (selected) Color.White else colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseTotalSummary(total: String?, count: Int?, label: String) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val foreground = if (dark) Color.White else colors.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (dark) SurfaceCardBlue else SurfaceCardBlueLight,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.32f))
    ) {
        Row(Modifier.heightIn(min = 70.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), color = foreground.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FinancialValue(total ?: "—", color = foreground, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            }
            Surface(
                color = PrimaryBlue.copy(alpha = if (dark) 0.22f else 0.10f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f))
            ) {
                Text(when (count) { null -> "—"; 1 -> "1 lançamento"; else -> "$count lançamentos" },
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = foreground,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ExpenseSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { contentDescription = "Buscar despesa por descrição ou valor" },
        interactionSource = interaction,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
        cursorBrush = SolidColor(PrimaryBlue),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { field ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .background(colors.surface, RoundedCornerShape(12.dp))
                .border(1.dp, if (focused) PrimaryBlue else Color.Transparent, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text("Buscar despesa...", color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    field()
                }
            }
        }
    )
}
