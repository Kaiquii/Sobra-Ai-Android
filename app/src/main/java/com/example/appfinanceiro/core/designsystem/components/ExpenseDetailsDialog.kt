package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.BackgroundDark
import com.example.appfinanceiro.core.designsystem.theme.BackgroundLight
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExpenseDetailsDialog(
    expense: Expense,
    categoryName: String,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogBackgroundColor = if (isDark) BackgroundDark else BackgroundLight
    val dialogTextColor = if (isDark) Color.White else Color.Black
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val formattedAmount = remember(expense.amount) { currencyFormatter.format(expense.amount) }
    val formattedDate = remember(expense.date) { formatExpenseDetailsDate(expense.date) }
    val typeLabel = remember(expense.type, expense.current_installment, expense.installments) {
        when {
            expense.type.equals("Parcelada", ignoreCase = true) &&
                expense.current_installment != null &&
                expense.installments != null -> {
                "Parcelada (${expense.current_installment}/${expense.installments})"
            }

            else -> expense.type
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBackgroundColor,
        titleContentColor = dialogTextColor,
        textContentColor = dialogTextColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Detalhes da despesa",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ExpenseSummaryHeader(
                    description = expense.description,
                    amount = formattedAmount,
                    categoryName = categoryName,
                    typeLabel = typeLabel,
                    textColor = dialogTextColor
                )

                DetailsGroup(
                    paymentSource = expense.payment_source ?: "Não informado",
                    date = formattedDate,
                    textColor = dialogTextColor
                )

                NotesDetailBox(
                    value = expense.notes?.takeIf { it.isNotBlank() } ?: "Sem observações",
                    textColor = dialogTextColor
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Fechar",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun ExpenseSummaryHeader(
    description: String,
    amount: String,
    categoryName: String,
    typeLabel: String,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrimaryBlue.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = description,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = amount,
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailChip(text = categoryName)
            DetailChip(text = typeLabel)
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = PrimaryBlue.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = PrimaryBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailsGroup(paymentSource: String, date: String, textColor: Color) {
    val isDark = isSystemInDarkTheme()
    val blockColor = if (isDark) Color(0xFF18222E) else Color(0xFFF2F4F7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = blockColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailLine(label = "Origem", value = paymentSource, textColor = textColor)
        HorizontalDivider(color = textColor.copy(alpha = 0.08f))
        DetailLine(label = "Data", value = date, textColor = textColor)
    }
}

@Composable
private fun DetailLine(label: String, value: String, textColor: Color) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.weight(0.7f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.3f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NotesDetailBox(value: String, textColor: Color) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val blockColor = if (isDark) Color(0xFF18222E) else Color(0xFFF2F4F7)
    val scrollState = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Observações",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .background(
                    color = blockColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            Text(
                text = value,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp
            )
        }
    }
}

private fun formatExpenseDetailsDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val output = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val date = parser.parse(dateString)
        if (date != null) output.format(date) else dateString
    } catch (e: Exception) {
        dateString
    }
}
