package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.feature.relatorios.utils.categoryColor
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency

@Composable
fun CategoryExpensesCard(
    totalExpense: Double,
    categories: List<CategoryReportResponse>
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Gastos por Categoria",
                color = TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatCurrency(totalExpense),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "Sem categorias para esse mês.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            } else {
                categories.sortedByDescending { it.total_amount }.forEachIndexed { index, item ->
                    CategoryProgressItem(
                        color = categoryColor(index),
                        name = item.category_name,
                        percentage = item.percentage.toFloat(),
                        amount = item.total_amount
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressItem(
    color: Color,
    name: String,
    percentage: Float,
    amount: Double
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${percentage.toInt()}%",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(TextMuted.copy(alpha = 0.18f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = formatCurrency(amount),
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}
