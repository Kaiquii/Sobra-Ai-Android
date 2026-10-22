package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.feature.relatorios.ReportRange
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency

@Composable
fun IncomeVsExpenseCard(
    summaryData: SummaryResponse?,
    chartData: List<ChartReportResponse>,
    selectedRange: ReportRange,
    currentMonth: Int,
    onRangeSelected: (ReportRange) -> Unit
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val isCompact = maxWidth < 360.dp

                if (isCompact) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Renda vs Despesas",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Saldo: ${formatCurrency(summaryData?.total_geral_disponivel ?: 0.0)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LegendItem("Renda", PrimaryBlue)
                            LegendItem("Despesa", DangerRed)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Renda vs Despesas",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Saldo: ${formatCurrency(summaryData?.total_geral_disponivel ?: 0.0)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompact) 22.sp else 28.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            LegendItem("Renda", PrimaryBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            LegendItem("Despesa", DangerRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ReportRangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (chartData.isEmpty()) {
                Text(
                    text = "Sem dados do gráfico.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            } else {
                ReportsBarChart(
                    data = chartData,
                    selectedRange = selectedRange,
                    currentMonth = currentMonth
                )
            }
        }
    }
}
