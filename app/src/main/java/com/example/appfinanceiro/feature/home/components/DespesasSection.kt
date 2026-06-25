package com.example.appfinanceiro.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Expense

@Composable
fun DespesasHeaderSection(
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    expenses: List<Expense>,
    onCategoryFilterClick: () -> Unit,
    onPaymentSourceFilterClick: () -> Unit,
    isCategoryFiltered: Boolean,
    isPaymentSourceFiltered: Boolean,
    selectedCategoryName: String?,
    selectedPaymentSourceName: String?,
    onClearCategoryFilter: () -> Unit,
    onClearPaymentSourceFilter: () -> Unit,
    onAddClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val hasActiveFilters = isCategoryFiltered || isPaymentSourceFiltered

    Column(
        modifier = Modifier.alpha(if (isLoading && expenses.isNotEmpty()) 0.5f else 1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Despesas",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton(
                    label = "Origem",
                    isActive = isPaymentSourceFiltered,
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = onPaymentSourceFilterClick
                )

                FilterButton(
                    label = "Categoria",
                    isActive = isCategoryFiltered,
                    icon = Icons.Default.FilterList,
                    onClick = onCategoryFilterClick
                )
            }
        }

        if (hasActiveFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedPaymentSourceName != null) {
                    ActiveFilterChip(
                        label = "Origem: $selectedPaymentSourceName",
                        onClear = onClearPaymentSourceFilter
                    )
                }

                if (selectedCategoryName != null) {
                    ActiveFilterChip(
                        label = "Categoria: $selectedCategoryName",
                        onClear = onClearCategoryFilter
                    )
                }
            }
        }

        if (expenses.isEmpty() && isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (expenses.isEmpty() && errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
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
        } else if (expenses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasActiveFilters) {
                        "Nenhuma despesa encontrada para estes filtros."
                    } else {
                        "Nenhuma despesa neste mês."
                    },
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onAddClick) {
                    Text("Adicionar despesa", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddExpenseButton(onAddClick: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TextMuted.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onAddClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = "Adicionar",
                tint = textColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(text = "Adicionar Despesa", color = textColor)
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    isActive: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val cardBg = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .background(
                if (isActive) PrimaryBlue.copy(alpha = 0.2f) else cardBg,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) PrimaryBlue else TextMuted,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = label,
            color = if (isActive) PrimaryBlue else TextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(PrimaryBlue.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .clickable { onClear() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = PrimaryBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "x",
            color = PrimaryBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExpenseItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    categoryName: String,
    paymentSource: String,
    type: String,
    date: String,
    value: String,
    notes: String?,
    onViewClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surface
    val sourceColor = paymentSourceColor(paymentSource)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconColor.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = categoryName,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniChip(label = type, color = TextMuted)

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = date,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )

                if (!notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    NoteIndicatorIcon()
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onViewClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Visualizar despesa",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .background(sourceColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Fonte",
                    tint = sourceColor,
                    modifier = Modifier.size(11.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = paymentSource,
                    color = sourceColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NoteIndicatorIcon() {
    Box(
        modifier = Modifier
            .background(PrimaryBlue.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Despesa com observação",
            tint = PrimaryBlue,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun MiniChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp)
    }
}

private fun paymentSourceColor(paymentSource: String): Color {
    return when {
        paymentSource.equals("Salario", ignoreCase = true) ||
                paymentSource.equals("Salário", ignoreCase = true) -> PrimaryBlue

        paymentSource.equals("Adiantamento", ignoreCase = true) -> Color(0xFF8B5CF6)

        paymentSource.equals("Renda Extra", ignoreCase = true) -> GreenPositive

        else -> TextMuted
    }
}
