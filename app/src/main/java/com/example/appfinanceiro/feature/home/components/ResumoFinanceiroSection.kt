package com.example.appfinanceiro.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardBlue
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardBlueLight
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardPurple
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardPurpleLight
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.feature.home.components.income.EditableIncomeSummaryCard
import com.example.appfinanceiro.feature.home.components.income.IncomeDeleteDialog
import com.example.appfinanceiro.feature.home.components.income.IncomeDeleteState
import com.example.appfinanceiro.feature.home.components.income.IncomeEditorDialog
import com.example.appfinanceiro.feature.home.components.income.IncomeEditorState
import com.example.appfinanceiro.feature.home.utils.formatCurrency

@Composable
fun ResumoFinanceiroSection(
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    data: SummaryResponse?,
    salarioIncome: Income?,
    adiantamentoIncome: Income?,
    rendaExtraIncome: Income?,
    token: String?,
    month: Int,
    year: Int,
    onRefresh: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surface
    val cardPurple = if (isDark) SurfaceCardPurple else SurfaceCardPurpleLight
    val cardBlue = if (isDark) SurfaceCardBlue else SurfaceCardBlueLight
    val alertColor = Color(0xFFFF6B6B)

    var editorState by remember { mutableStateOf<IncomeEditorState?>(null) }
    var deleteState by remember { mutableStateOf<IncomeDeleteState?>(null) }

    if (editorState != null) {
        IncomeEditorDialog(
            state = editorState!!,
            token = token,
            month = month,
            year = year,
            onDismiss = { editorState = null },
            onSuccess = {
                editorState = null
                onRefresh()
            }
        )
    }

    if (deleteState != null) {
        IncomeDeleteDialog(
            state = deleteState!!,
            token = token,
            onDismiss = { deleteState = null },
            onSuccess = {
                deleteState = null
                onRefresh()
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Resumo Financeiro",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (isLoading && data != null) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = PrimaryBlue,
                    strokeWidth = 2.dp
                )
            }
        }

        if (data == null && errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(errorMessage, color = TextMuted, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onRetry) {
                    Text(
                        text = "Tentar novamente",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (data == null && isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier.alpha(if (isLoading) 0.5f else 1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val salario = data?.salario ?: 0.0
                val adiantamento = data?.adiantamento ?: 0.0
                val rendaExtraDisp = data?.restante_renda_extra ?: 0.0
                val gastoSalario = data?.total_gasto_salario ?: 0.0
                val gastoAdiant = data?.total_gasto_adiantamento ?: 0.0
                val restSalario = data?.restante_salario ?: 0.0
                val restAdiant = data?.restante_adiantamento ?: 0.0
                val totalRecebido = data?.total_income ?: 0.0
                val totalGasto = data?.total_expense ?: 0.0
                val totalDisp = data?.total_geral_disponivel ?: 0.0

                HighlightSummaryCard(
                    title = "Total Geral Disponível",
                    subtitle = if (totalDisp < 0) "Atenção: saldo negativo no mês" else "Saldo livre no mês",
                    value = formatCurrency(totalDisp),
                    bgColor = cardBlue,
                    valueColor = if (totalDisp < 0) alertColor else PrimaryBlue
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
                        modifier = Modifier.weight(1f),
                        onCreate = {
                            editorState = IncomeEditorState(
                                title = "salário",
                                source = "Salario"
                            )
                        },
                        onEdit = { income ->
                            editorState = IncomeEditorState(
                                title = "salário",
                                source = "Salario",
                                existingIncome = income
                            )
                        },
                        onDelete = { income ->
                            deleteState = IncomeDeleteState(
                                income = income,
                                label = "o salário"
                            )
                        }
                    )

                    EditableIncomeSummaryCard(
                        title = "Adiantamento",
                        value = formatCurrency(adiantamento),
                        bgColor = cardBg,
                        valueColor = textColor,
                        existingIncome = adiantamentoIncome,
                        modifier = Modifier.weight(1f),
                        onCreate = {
                            editorState = IncomeEditorState(
                                title = "adiantamento",
                                source = "Adiantamento"
                            )
                        },
                        onEdit = { income ->
                            editorState = IncomeEditorState(
                                title = "adiantamento",
                                source = "Adiantamento",
                                existingIncome = income
                            )
                        },
                        onDelete = { income ->
                            deleteState = IncomeDeleteState(
                                income = income,
                                label = "o adiantamento"
                            )
                        }
                    )
                }

                EditableIncomeSummaryCard(
                    title = "Renda Extra (Disponível)",
                    value = formatCurrency(rendaExtraDisp),
                    bgColor = cardBg,
                    valueColor = GreenPositive,
                    existingIncome = rendaExtraIncome,
                    icon = Icons.Default.TrendingUp,
                    onCreate = {
                        editorState = IncomeEditorState(
                            title = "renda extra",
                            source = "Renda Extra"
                        )
                    },
                    onEdit = { income ->
                        editorState = IncomeEditorState(
                            title = "renda extra",
                            source = "Renda Extra",
                            existingIncome = income
                        )
                    },
                    onDelete = { income ->
                        deleteState = IncomeDeleteState(
                            income = income,
                            label = "a renda extra"
                        )
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "Restante Salário",
                        value = formatCurrency(restSalario),
                        bgColor = cardPurple,
                        valueColor = if (restSalario < 0) alertColor else textColor,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Restante Adiant.",
                        value = formatCurrency(restAdiant),
                        bgColor = cardPurple,
                        valueColor = if (restAdiant < 0) alertColor else textColor,
                        modifier = Modifier.weight(1f)
                    )
                }

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
            }
        }
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
            Text(
                text = value,
                color = valueColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
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
                Text(
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
