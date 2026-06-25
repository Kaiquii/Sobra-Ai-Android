package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val colorScheme = MaterialTheme.colorScheme
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
        containerColor = colorScheme.surface,
        titleContentColor = colorScheme.onSurface,
        textContentColor = colorScheme.onSurface,
        title = {
            Text(
                text = "Detalhes da despesa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow(label = "Descrição", value = expense.description)
                DetailRow(label = "Valor", value = formattedAmount)
                DetailRow(label = "Categoria", value = categoryName)
                DetailRow(label = "Origem do pagamento", value = expense.payment_source ?: "Não informado")
                DetailRow(label = "Tipo", value = typeLabel)
                DetailRow(label = "Data", value = formattedDate)

                Spacer(modifier = Modifier.height(4.dp))

                NotesDetailBox(
                    value = expense.notes?.takeIf { it.isNotBlank() } ?: "Sem observações"
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
private fun NotesDetailBox(value: String) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column {
        Text(
            text = "Observações",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .background(
                    color = colorScheme.background.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            Text(
                text = value,
                color = colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = label,
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
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
