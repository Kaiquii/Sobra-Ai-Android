package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.feature.relatorios.utils.categoryColor
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency

data class CategoryDonutSlice(val colorIndex: Int, val startAngle: Float, val sweepAngle: Float)

/** Geometry only: keep the API amounts/percentages unchanged in the legend. */
fun categoryDonutSlices(orderedCategories: List<CategoryReportResponse>): List<CategoryDonutSlice> {
    // A pie cannot represent negative adjustments or non-finite values honestly.
    if (orderedCategories.any { !it.total_amount.isFinite() || it.total_amount < 0.0 }) return emptyList()
    val maximum = orderedCategories.maxOfOrNull { it.total_amount } ?: return emptyList()
    if (maximum <= 0.0) return emptyList()
    // Scaling first avoids overflowing a sum of otherwise finite amounts.
    val sum = orderedCategories.sumOf { it.total_amount / maximum }
    var angle = -90.0
    return orderedCategories.mapIndexedNotNull { index, category ->
        if (category.total_amount == 0.0) return@mapIndexedNotNull null
        val sweep = (category.total_amount / maximum) / sum * 360.0
        CategoryDonutSlice(index, angle.toFloat(), sweep.toFloat()).also { angle += sweep }
    }
}

/** Resolve a touch against the painted ring, excluding the hole and outer padding. */
fun categoryDonutHitTest(slices: List<CategoryDonutSlice>, x: Float, y: Float, diameter: Float): Int? {
    if (!diameter.isFinite() || diameter <= 0f || !x.isFinite() || !y.isFinite()) return null
    val dx = x - diameter / 2f
    val dy = y - diameter / 2f
    val radius = kotlin.math.sqrt(dx * dx + dy * dy)
    if (radius < diameter * 0.27f || radius > diameter * 0.46f) return null
    val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * 180.0 / kotlin.math.PI
    return slices.firstOrNull { slice ->
        val relative = ((angle - slice.startAngle) % 360.0 + 360.0) % 360.0
        slice.sweepAngle > 0f && relative < slice.sweepAngle
    }?.colorIndex
}

@Composable
fun CategoryDonutChart(
    slices: List<CategoryDonutSlice>,
    categories: List<CategoryReportResponse>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val select by rememberUpdatedState(onSelect)
    val selected = selectedIndex?.let { categories.getOrNull(it) }
    val selectedSlice = slices.firstOrNull { it.colorIndex == selectedIndex }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.widthIn(max = 192.dp).fillMaxWidth().aspectRatio(1f)
                .pointerInput(slices) {
                    detectTapGestures { point ->
                        select(categoryDonutHitTest(slices, point.x, point.y, minOf(size.width, size.height).toFloat()))
                    }
                }
                .semantics {
                    contentDescription = "Gráfico de gastos por categoria. Toque nas fatias ou selecione uma categoria na lista para consultar o valor."
                }
        ) {
            val diameter = size.minDimension
            val stroke = diameter * 0.19f
            val outerRadius = diameter * 0.46f
            val arcDiameter = outerRadius * 2f - stroke
            val inset = (diameter - arcDiameter) / 2f
            slices.forEach { slice ->
                val chosen = slice.colorIndex == selectedIndex
                val middle = (slice.startAngle + slice.sweepAngle / 2f) * kotlin.math.PI / 180.0
                val shift = if (chosen) 4.dp.toPx() else 0f
                val offset = Offset(kotlin.math.cos(middle).toFloat() * shift, kotlin.math.sin(middle).toFloat() * shift)
                val gap = minOf(0.8f, slice.sweepAngle * 0.12f)
                if (chosen) {
                    drawArc(categoryColor(slice.colorIndex).copy(alpha = 0.16f), slice.startAngle + gap / 2f,
                        slice.sweepAngle - gap, false, Offset(inset, inset) + offset,
                        Size(arcDiameter, arcDiameter), style = Stroke(stroke + 4.dp.toPx()))
                }
                val alpha = if (selectedSlice != null && !chosen) 0.55f else 1f
                drawArc(categoryColor(slice.colorIndex).copy(alpha = alpha), slice.startAngle + gap / 2f,
                    slice.sweepAngle - gap, false, Offset(inset, inset) + offset,
                    Size(arcDiameter, arcDiameter), style = Stroke(stroke))
            }
        }
        if (selected != null && selectedSlice != null) {
            val middle = (selectedSlice.startAngle + selectedSlice.sweepAngle / 2f) * kotlin.math.PI / 180.0
            val alignment = when {
                kotlin.math.sin(middle) < 0 && kotlin.math.cos(middle) >= 0 -> Alignment.TopEnd
                kotlin.math.sin(middle) < 0 -> Alignment.TopStart
                kotlin.math.cos(middle) >= 0 -> Alignment.BottomEnd
                else -> Alignment.BottomStart
            }
            Surface(
                onClick = { select(null) },
                modifier = Modifier.align(alignment).widthIn(max = 156.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(10.dp), color = colors.background,
                contentColor = colors.onBackground, shadowElevation = 4.dp,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(selected.category_name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(formatCurrency(selected.total_amount), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
