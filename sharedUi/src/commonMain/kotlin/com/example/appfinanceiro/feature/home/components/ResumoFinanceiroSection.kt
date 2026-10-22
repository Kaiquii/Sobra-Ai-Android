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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardBlue
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardBlueLight
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardPurple
import com.example.appfinanceiro.core.designsystem.theme.SurfaceCardPurpleLight
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.designsystem.components.FinancialValue
import com.example.appfinanceiro.core.designsystem.components.LocalFinancialPrivacy
import com.example.appfinanceiro.feature.home.utils.formatCurrency
import com.example.appfinanceiro.feature.home.components.income.EditableIncomeSummaryCard
import com.example.appfinanceiro.feature.home.components.income.IncomeDeleteDialog
import com.example.appfinanceiro.feature.home.components.income.IncomeDeleteState
import com.example.appfinanceiro.feature.home.components.income.IncomeEditorDialog
import com.example.appfinanceiro.feature.home.components.income.IncomeEditorState

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
    val valuesHidden = LocalFinancialPrivacy.current.valuesHidden

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
        Text(
            text = "Resumo Financeiro",
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

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
                AppLoadingIndicator(size = 40.dp, strokeWidth = 4.dp)
            }
        } else {
            FinanceSummaryContent(data, salarioIncome, adiantamentoIncome, rendaExtraIncome,
                onEdit = { source, income -> editorState = IncomeEditorState(
                    title = if (source == "Salario") "salário" else source.lowercase(),
                    source = source, existingIncome = income) },
                onDelete = { income, label -> deleteState = IncomeDeleteState(income = income, label = label) },
                formatCurrency = ::formatCurrency)
        }
    }
}
