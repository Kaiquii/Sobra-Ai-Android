package com.example.appfinanceiro.feature.relatorios.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.YearlySummaryResponse
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency

@Composable
fun YearSummarySection(yearlySummary: YearlySummaryResponse?) {
    Column {
        Text(
            text = "Resumo do Ano",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryInfoCard(
                modifier = Modifier.weight(1f),
                title = "ECONOMIA TOTAL",
                value = formatCurrency(yearlySummary?.economia_total ?: 0.0),
                subtitle = "Acumulado no ano",
                iconColor = PrimaryBlue,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                }
            )

            SummaryInfoCard(
                modifier = Modifier.weight(1f),
                title = "MÉDIA MENSAL",
                value = formatCurrency(yearlySummary?.media_mensal ?: 0.0),
                subtitle = "Média até ${yearlySummary?.year ?: ""}",
                iconColor = GreenPositive,
                icon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = GreenPositive
                    )
                }
            )
        }
    }
}

@Composable
private fun SummaryInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    iconColor: Color,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )


            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
