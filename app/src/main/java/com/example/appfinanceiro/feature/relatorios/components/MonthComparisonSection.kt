package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.MonthComparisonResponse
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency
import com.example.appfinanceiro.feature.relatorios.utils.monthShortName
import java.util.Locale
import kotlin.math.abs

@Composable
fun MonthComparisonSection(
    data: MonthComparisonResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    currentMonthIndex: Int,
    currentYear: Int,
    compareMonthIndex: Int,
    compareYear: Int,
    onPrevCompareClick: () -> Unit,
    onNextCompareClick: () -> Unit
) {
    val currentLabel = monthYearLabel(currentMonthIndex + 1, currentYear)
    val comparedLabel = monthYearLabel(compareMonthIndex + 1, compareYear)
    val categoryBreakdowns = remember(data) {
        data?.categorias
            ?.sortedByDescending { abs(it.diferenca) }
            ?.map {
                ComparisonBreakdown(
                    name = it.categoria_nome,
                    currentValue = it.valor_atual,
                    comparedValue = it.valor_comparado,
                    difference = it.diferenca,
                    percent = it.percentual,
                    status = it.status
                )
            }
            .orEmpty()
    }
    val paymentSourceBreakdowns = remember(data) {
        data?.fontes_pagamento
            ?.sortedByDescending { abs(it.diferenca) }
            ?.map {
                ComparisonBreakdown(
                    name = it.fonte_pagamento,
                    currentValue = it.valor_atual,
                    comparedValue = it.valor_comparado,
                    difference = it.diferenca,
                    percent = it.percentual,
                    status = it.status
                )
            }
            .orEmpty()
    }
    val expenseTypeBreakdowns = remember(data) {
        data?.tipos_despesa
            ?.sortedByDescending { abs(it.diferenca) }
            ?.map {
                ComparisonBreakdown(
                    name = it.tipo,
                    currentValue = it.valor_atual,
                    comparedValue = it.valor_comparado,
                    difference = it.diferenca,
                    percent = it.percentual,
                    status = it.status
                )
            }
            .orEmpty()
    }
    val observations = remember(data) {
        data?.insights?.map { formatObservation(it) }.orEmpty()
    }

    Column {
        Text(
            text = "Comparativo Mensal",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                PeriodHeader(
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    onPrevCompareClick = onPrevCompareClick,
                    onNextCompareClick = onNextCompareClick
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue,
                        trackColor = PrimaryBlue.copy(alpha = 0.12f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Atualizando comparativo...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    return@Column
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = DangerRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    return@Column
                }

                if (data == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sem dados do comparativo mensal.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    return@Column
                }

                Spacer(modifier = Modifier.height(18.dp))

                ComparisonMetricRow(
                    title = "Receitas",
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    currentValue = data.resumo.receitas_atual,
                    comparedValue = data.resumo.receitas_comparado,
                    difference = data.resumo.diferenca_receitas,
                    percent = data.resumo.percentual_receitas,
                    status = data.resumo.status_receitas,
                    positiveWhenUp = true
                )

                SectionDivider()

                ComparisonMetricRow(
                    title = "Despesas",
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    currentValue = data.resumo.despesas_atual,
                    comparedValue = data.resumo.despesas_comparado,
                    difference = data.resumo.diferenca_despesas,
                    percent = data.resumo.percentual_despesas,
                    status = data.resumo.status_despesas,
                    positiveWhenUp = false
                )

                SectionDivider()

                ComparisonMetricRow(
                    title = "Saldo",
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    currentValue = data.resumo.saldo_atual,
                    comparedValue = data.resumo.saldo_comparado,
                    difference = data.resumo.diferenca_saldo,
                    percent = data.resumo.percentual_saldo,
                    status = data.resumo.status_saldo,
                    positiveWhenUp = true
                )

                BreakdownSection(
                    title = "Categorias",
                    emptyText = "Sem categorias no comparativo.",
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    positiveWhenUp = false,
                    items = categoryBreakdowns
                )

                BreakdownSection(
                    title = "Fontes de pagamento",
                    emptyText = "Sem fontes no comparativo.",
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    positiveWhenUp = false,
                    items = paymentSourceBreakdowns
                )

                BreakdownSection(
                    title = "Tipos de despesa",
                    emptyText = "Sem tipos no comparativo.",
                    currentLabel = currentLabel,
                    comparedLabel = comparedLabel,
                    positiveWhenUp = false,
                    items = expenseTypeBreakdowns
                )

                if (observations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Observações",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    observations.forEach { insight ->
                        Text(
                            text = "- $insight",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodHeader(
    currentLabel: String,
    comparedLabel: String,
    onPrevCompareClick: () -> Unit,
    onNextCompareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mês principal",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Comparar com",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPrevCompareClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Mês anterior",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = comparedLabel,
                    color = PrimaryBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(92.dp)
                )
                IconButton(
                    onClick = onNextCompareClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Próximo mês",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    title: String,
    currentLabel: String,
    comparedLabel: String,
    currentValue: Double,
    comparedValue: Double,
    difference: Double,
    percent: Double,
    status: String,
    positiveWhenUp: Boolean
) {
    val statusColor = comparisonStatusColor(status, positiveWhenUp)

    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ValueColumn(
                modifier = Modifier.weight(1f),
                label = currentLabel,
                value = formatCurrency(currentValue)
            )
            ValueColumn(
                modifier = Modifier.weight(1f),
                label = comparedLabel,
                value = formatCurrency(comparedValue)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${statusLabel(status)} ${formatSignedCurrency(difference)} (${formatPercent(percent)})",
            color = statusColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ValueColumn(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BreakdownSection(
    title: String,
    emptyText: String,
    currentLabel: String,
    comparedLabel: String,
    positiveWhenUp: Boolean,
    items: List<ComparisonBreakdown>
) {
    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (items.isEmpty()) {
        Text(text = emptyText, color = TextMuted, fontSize = 13.sp)
    } else {
        items.forEachIndexed { index, item ->
            BreakdownRow(
                item = item,
                currentLabel = currentLabel,
                comparedLabel = comparedLabel,
                positiveWhenUp = positiveWhenUp
            )
            if (index != items.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    item: ComparisonBreakdown,
    currentLabel: String,
    comparedLabel: String,
    positiveWhenUp: Boolean
) {
    val statusColor = comparisonStatusColor(item.status, positiveWhenUp)

    Column {
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ValueColumn(
                modifier = Modifier.weight(1f),
                label = currentLabel,
                value = formatCurrency(item.currentValue)
            )
            ValueColumn(
                modifier = Modifier.weight(1f),
                label = comparedLabel,
                value = formatCurrency(item.comparedValue)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${statusLabel(item.status)} ${formatSignedCurrency(item.difference)} (${formatPercent(item.percent)})",
            color = statusColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
    Spacer(modifier = Modifier.height(14.dp))
}

private data class ComparisonBreakdown(
    val name: String,
    val currentValue: Double,
    val comparedValue: Double,
    val difference: Double,
    val percent: Double,
    val status: String
)

private fun monthYearLabel(month: Int, year: Int): String {
    return "${monthShortName(month)}/$year"
}

private fun formatSignedCurrency(value: Double): String {
    return when {
        value > 0.0 -> "+${formatCurrency(value)}"
        value < 0.0 -> "-${formatCurrency(abs(value))}"
        else -> formatCurrency(0.0)
    }
}

private fun formatPercent(value: Double): String {
    return "${String.format(Locale.forLanguageTag("pt-BR"), "%.1f", abs(value))}%"
}

private fun statusLabel(status: String): String {
    return when (status.lowercase()) {
        "subiu" -> "Subiu"
        "caiu" -> "Caiu"
        "melhorou" -> "Melhorou"
        "piorou" -> "Piorou"
        else -> "Igual"
    }
}

private fun comparisonStatusColor(status: String, positiveWhenUp: Boolean): Color {
    return when (status.lowercase()) {
        "melhorou" -> GreenPositive
        "piorou" -> DangerRed
        "subiu" -> if (positiveWhenUp) GreenPositive else DangerRed
        "caiu" -> if (positiveWhenUp) DangerRed else GreenPositive
        else -> TextMuted
    }
}

private fun formatObservation(text: String): String {
    return text
        .replaceWord("voce", "você")
        .replaceWord("relacao", "relação")
        .replaceWord("mes", "mês")
        .replaceWord("Alimentacao", "Alimentação")
        .replaceWord("Educacao", "Educação")
        .replaceWord("Saude", "Saúde")
}

private fun String.replaceWord(source: String, replacement: String): String {
    return replace(Regex("\\b$source\\b", RegexOption.IGNORE_CASE), replacement)
}
