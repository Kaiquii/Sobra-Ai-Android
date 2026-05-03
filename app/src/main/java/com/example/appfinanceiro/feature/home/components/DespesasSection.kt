package com.example.appfinanceiro.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.feature.home.utils.formatCurrency
import com.example.appfinanceiro.feature.home.utils.formatExpenseDate
import com.example.appfinanceiro.feature.home.utils.getCategoryIconAndColor

@Composable
fun DespesasSection(
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    expenses: List<Expense>,
    categoriesMap: Map<Int, String>,
    onCategoryFilterClick: () -> Unit,
    onPaymentSourceFilterClick: () -> Unit,
    isCategoryFiltered: Boolean,
    isPaymentSourceFiltered: Boolean,
    onAddClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surface

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
                Row(
                    modifier = Modifier
                        .background(
                            if (isPaymentSourceFiltered) {
                                PrimaryBlue.copy(alpha = 0.2f)
                            } else {
                                cardBg
                            },
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onPaymentSourceFilterClick() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Origem",
                        tint = if (isPaymentSourceFiltered) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Origem",
                        color = if (isPaymentSourceFiltered) PrimaryBlue else TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .background(
                            if (isCategoryFiltered) {
                                PrimaryBlue.copy(alpha = 0.2f)
                            } else {
                                cardBg
                            },
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onCategoryFilterClick() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Categoria",
                        tint = if (isCategoryFiltered) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Categoria",
                        color = if (isCategoryFiltered) PrimaryBlue else TextMuted,
                        fontSize = 12.sp
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
                Text(
                    text = errorMessage,
                    color = TextMuted,
                    fontSize = 14.sp
                )

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma despesa encontrada.",
                    color = TextMuted
                )
            }
        } else {
            expenses.forEach { expense ->
                val categoryName = categoriesMap[expense.category_id] ?: "Outros"
                val (icon, color) = getCategoryIconAndColor(categoryName)
                val formattedDate = formatExpenseDate(expense.date)

                val typeText =
                    if (
                        expense.type.equals("Parcelada", ignoreCase = true) &&
                        expense.installments != null &&
                        expense.current_installment != null
                    ) {
                        "Parc. ${expense.current_installment}/${expense.installments}"
                    } else {
                        expense.type
                    }

                ExpenseItem(
                    icon = icon,
                    iconColor = color,
                    title = expense.description,
                    categoryName = categoryName,
                    paymentSource = expense.payment_source ?: "Não informado",
                    type = typeText,
                    date = formattedDate,
                    value = "- ${formatCurrency(expense.amount)}"
                )
            }
        }

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

                Text(
                    text = "Adicionar Despesa",
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    categoryName: String,
    paymentSource: String,
    type: String,
    date: String,
    value: String
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = categoryName,
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(TextMuted.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = type,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "• $date",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = value,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Fonte",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(12.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = paymentSource,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
