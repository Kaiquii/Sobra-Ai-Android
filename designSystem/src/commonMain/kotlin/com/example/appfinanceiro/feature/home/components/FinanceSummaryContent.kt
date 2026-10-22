package com.example.appfinanceiro.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.appfinanceiro.core.designsystem.components.*
import com.example.appfinanceiro.core.designsystem.theme.*
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.feature.home.components.income.EditableIncomeSummaryCard

@Composable
fun FinanceSummaryContent(data: SummaryResponse?, salarioIncome: Income?, adiantamentoIncome: Income?, rendaExtraIncome: Income?,
    onEdit: (String, Income?) -> Unit, onDelete: (Income, String) -> Unit,
    formatCurrency: (Double) -> String) {
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surface
    val cardPurple = if (isDark) SurfaceCardPurple else SurfaceCardPurpleLight
    val cardBlue = if (isDark) SurfaceCardBlue else SurfaceCardBlueLight
    val alertColor = Color(0xFFFF6B6B)
    val valuesHidden = LocalFinancialPrivacy.current.valuesHidden

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val salario = data?.salario ?: 0.0
                val adiantamento = data?.adiantamento ?: 0.0
                val rendaExtra = data?.renda_extra_amt ?: 0.0
                val gastoSalario = data?.total_gasto_salario ?: 0.0
                val gastoAdiant = data?.total_gasto_adiantamento ?: 0.0
                val gastoRendaExtra = data?.total_gasto_renda_extra ?: 0.0
                val restSalario = data?.restante_salario ?: 0.0
                val restAdiant = data?.restante_adiantamento ?: 0.0
                val restRendaExtra = data?.restante_renda_extra ?: 0.0
                val totalRecebido = data?.total_income ?: 0.0
                val totalGasto = data?.total_expense ?: 0.0
                val totalDisp = data?.total_geral_disponivel ?: 0.0

                HighlightSummaryCard(
                    title = "Total Geral Disponível",
                    subtitle = if (totalDisp < 0)
                        "Atenção: saldo negativo no mês" else "Saldo livre no mês",
                    value = formatCurrency(totalDisp),
                    bgColor = cardBlue,
                    valueColor = if (!valuesHidden && totalDisp < 0) alertColor else PrimaryBlue
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "Total Recebido",
                        value = formatCurrency(totalRecebido),
                        bgColor = cardBg,
                        valueColor = textColor,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Total Gasto",
                        value = formatCurrency(totalGasto),
                        bgColor = cardBg,
                        valueColor = textColor,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditableIncomeSummaryCard(
                        title = "Salário",
                        value = formatCurrency(salario),
                        bgColor = cardBg,
                        valueColor = textColor,
                        existingIncome = salarioIncome,
                        actionsEnabled = !valuesHidden,
                        modifier = Modifier.weight(1f),
                        onCreate = {
                            onEdit("Salario", null)
                        },
                        onEdit = { income ->
                            onEdit("Salario", income)
                        },
                        onDelete = { income ->
                            onDelete(income, "o salário")
                        }
                    )

                    EditableIncomeSummaryCard(
                        title = "Adiantamento",
                        value = formatCurrency(adiantamento),
                        bgColor = cardBg,
                        valueColor = textColor,
                        existingIncome = adiantamentoIncome,
                        actionsEnabled = !valuesHidden,
                        modifier = Modifier.weight(1f),
                        onCreate = {
                            onEdit("Adiantamento", null)
                        },
                        onEdit = { income ->
                            onEdit("Adiantamento", income)
                        },
                        onDelete = { income ->
                            onDelete(income, "o adiantamento")
                        }
                    )
                }

                EditableIncomeSummaryCard(
                    title = "Renda Extra",
                    value = formatCurrency(rendaExtra),
                    bgColor = cardBg,
                    valueColor = GreenPositive,
                    existingIncome = rendaExtraIncome,
                    actionsEnabled = !valuesHidden,
                    icon = Icons.Default.TrendingUp,
                    onCreate = {
                        onEdit("Renda Extra", null)
                    },
                    onEdit = { income ->
                        onEdit("Renda Extra", income)
                    },
                    onDelete = { income ->
                        onDelete(income, "a renda extra")
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "Restante Salário",
                        value = formatCurrency(restSalario),
                        bgColor = cardPurple,
                        valueColor = if (!valuesHidden && restSalario < 0) alertColor else textColor,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Restante Adiant.",
                        value = formatCurrency(restAdiant),
                        bgColor = cardPurple,
                        valueColor = if (!valuesHidden && restAdiant < 0) alertColor else textColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                SummaryCard(
                    title = "Restante Renda Extra",
                    value = formatCurrency(restRendaExtra),
                    bgColor = cardPurple,
                    valueColor = if (!valuesHidden && restRendaExtra < 0) alertColor else textColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "Gasto Salário",
                        value = formatCurrency(gastoSalario),
                        bgColor = cardBg,
                        valueColor = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Gasto Adiant.",
                        value = formatCurrency(gastoAdiant),
                        bgColor = cardBg,
                        valueColor = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                SummaryCard(
                    title = "Gasto Renda Extra",
                    value = formatCurrency(gastoRendaExtra),
                    bgColor = cardBg,
                    valueColor = textColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
}

@Composable
private fun HighlightSummaryCard(
    title: String,
    subtitle: String,
    value: String,
    bgColor: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FinancialValue(
                text = value,
                color = valueColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FinancialValue(subtitle, color = TextMuted, fontSize = 11.sp)
        }

        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = valueColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    valueColor: Color,
    valueSize: TextUnit = 18.sp,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FinancialValue(
                    text = value,
                    color = valueColor,
                    fontSize = valueSize,
                    fontWeight = FontWeight.Bold
                )

                if (icon != null) {
                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = valueColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
