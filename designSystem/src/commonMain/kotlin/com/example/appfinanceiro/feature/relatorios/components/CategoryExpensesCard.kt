package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.components.FinancialValue
import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.feature.relatorios.utils.categoryColor
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency
import com.example.appfinanceiro.feature.relatorios.utils.formatReportPercent

@Composable
fun CategoryExpensesCard(totalExpense: Double, categories: List<CategoryReportResponse>) {
    val orderedCategories = remember(categories) { categories.sortedByDescending { it.total_amount } }
    val slices = remember(orderedCategories) { categoryDonutSlices(orderedCategories) }
    var selectedIndex by remember(categories) { mutableStateOf<Int?>(null) }
    val select: (Int?) -> Unit = { index -> selectedIndex = if (selectedIndex == index) null else index }
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
        Column(Modifier.padding(16.dp)) {
            Text("Gastos por Categoria", color = colors.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            FinancialValue(formatCurrency(totalExpense), color = colors.onSurface,
                fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            if (categories.isEmpty()) {
                Text("Sem categorias para esse mês.", color = colors.onSurfaceVariant, fontSize = 14.sp)
            } else {
                if (slices.isNotEmpty()) {
                    CategoryDonutChart(slices, orderedCategories, selectedIndex, select)
                    Spacer(Modifier.height(10.dp))
                } else if (orderedCategories.any { !it.total_amount.isFinite() || it.total_amount < 0.0 }) {
                    Text("Gráfico indisponível para estes valores. Consulte as categorias abaixo.",
                        color = colors.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                }
                orderedCategories.forEachIndexed { index, item ->
                    CategoryProgressItem(
                        color = categoryColor(index), item = item, isSelected = selectedIndex == index,
                        onSelect = if (slices.any { it.colorIndex == index }) ({ select(index) }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressItem(color: Color, item: CategoryReportResponse, isSelected: Boolean, onSelect: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val percentage = item.percentage.takeIf { it.isFinite() } ?: 0.0
    val textPercentage = "${formatReportPercent(percentage)}%"
    val selectionShape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(selectionShape)
            .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (isSelected) color.copy(alpha = 0.35f) else Color.Transparent, selectionShape)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .then(if (onSelect != null) Modifier.clickable(
                onClickLabel = if (isSelected) "Desmarcar categoria" else "Selecionar categoria",
                onClick = onSelect
            ) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val largeFont = LocalDensity.current.fontScale > 1.3f
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackValue = maxWidth < 260.dp || largeFont
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.category_name, color = colors.onSurface, fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false))
                        Text(textPercentage, color = colors.onSurfaceVariant, fontSize = 11.sp)
                    }
                    if (!stackValue) FinancialValue(formatCurrency(item.total_amount),
                        color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                if (stackValue) FinancialValue(formatCurrency(item.total_amount),
                    color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 14.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(5.dp).background(colors.onSurface.copy(alpha = 0.09f), RoundedCornerShape(50))) {
            Box(Modifier.fillMaxWidth((percentage / 100.0).toFloat().coerceIn(0f, 1f))
                .height(5.dp).background(color, RoundedCornerShape(50)))
        }
    }
}
