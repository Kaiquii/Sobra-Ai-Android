package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.feature.relatorios.ReportRange
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency
import com.example.appfinanceiro.feature.relatorios.utils.monthShortName

@Composable
fun ReportsBarChart(
    data: List<ChartReportResponse>,
    selectedRange: ReportRange,
    currentMonth: Int
) {
    val scrollState = rememberScrollState()

    val visibleData = remember(data, selectedRange, currentMonth) {
        when (selectedRange) {
            ReportRange.ONE_MONTH -> {
                data.filter { it.month == currentMonth }
            }

            ReportRange.SIX_MONTHS -> {
                val startMonth = (currentMonth - 3).coerceAtLeast(1)
                val endMonth = (currentMonth + 2).coerceAtMost(12)
                data.filter { it.month in startMonth..endMonth }
            }

            ReportRange.ONE_YEAR -> {
                data
            }
        }
    }

    val maxValue = (visibleData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0)
        .coerceAtLeast(1.0)

    var selectedMonth by remember(visibleData, currentMonth) {
        mutableStateOf(visibleData.firstOrNull { it.month == currentMonth })
    }

    val groupWidth = when (selectedRange) {
        ReportRange.ONE_MONTH -> 140.dp
        ReportRange.SIX_MONTHS -> 96.dp
        ReportRange.ONE_YEAR -> 82.dp
    }

    Column {
        selectedMonth?.let { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = monthShortName(item.month),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Renda: ${formatCurrency(item.income)}",
                            color = PrimaryBlue,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Despesa: ${formatCurrency(item.expense)}",
                            color = DangerRed,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Saldo: ${formatCurrency(item.income - item.expense)}",
                            color = if (item.income - item.expense >= 0) GreenPositive else DangerRed,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(onClick = { selectedMonth = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar detalhes",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            visibleData.forEach { item ->
                val incomeRatio = (item.income / maxValue).toFloat()
                val expenseRatio = (item.expense / maxValue).toFloat()
                val isSelected = selectedMonth?.month == item.month
                val isCurrentMonth = item.month == currentMonth

                Column(
                    modifier = Modifier.width(groupWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .height(220.dp)
                            .background(
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = PrimaryBlue.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(incomeRatio)
                                .background(
                                    color = if (isSelected) PrimaryBlue.copy(alpha = 0.85f) else PrimaryBlue,
                                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                )
                                .clickable { selectedMonth = item }
                        )

                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(expenseRatio)
                                .background(
                                    color = if (isSelected) DangerRed.copy(alpha = 0.85f) else DangerRed,
                                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                )
                                .clickable { selectedMonth = item }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = monthShortName(item.month),
                        color = when {
                            isSelected -> PrimaryBlue
                            isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                            else -> TextMuted
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
