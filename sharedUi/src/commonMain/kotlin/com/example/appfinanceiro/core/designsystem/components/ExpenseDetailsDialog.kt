package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.expenseDetailBlock
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.PaymentSplit
import com.example.appfinanceiro.core.network.displayDescription
import br.com.sobraai.ui.runtime.NumberFormat
import br.com.sobraai.ui.runtime.SimpleDateFormat
import br.com.sobraai.ui.runtime.Locale

@Composable
fun ExpenseDetailsDialog(
    expense: Expense,
    categoryName: String,
    onDismiss: () -> Unit,
    onAdvanceClick: (() -> Unit)? = null,
    onChangeAdvanceDateClick: (() -> Unit)? = null,
    onRemoveAdvanceClick: (() -> Unit)? = null
) {
    val dialogBackgroundColor = MaterialTheme.colorScheme.background
    val dialogTextColor = MaterialTheme.colorScheme.onBackground
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(expense.amount)
    val formattedDate = remember(expense.date) { formatExpenseDetailsDate(expense.date.orEmpty()) }
    val paidAtLabel = remember(expense.is_paid, expense.paid_at) {
        expense.paid_at
            ?.takeIf { expense.is_paid }
            ?.let(::formatExpensePaidAt)
    }
    val advancedAtLabel = remember(expense.isAdvanced, expense.advancedAt) {
        expense.advancedAt
            ?.takeIf { expense.isAdvanced }
            ?.let(::formatExpenseAdvancedAt)
    }
    val typeLabel = remember(expense.type, expense.current_installment, expense.installments) {
        when {
            expense.type.equals("Parcelada", ignoreCase = true) &&
                expense.current_installment != null &&
                expense.installments != null -> {
                "Parcelada (${expense.current_installment}/${expense.installments})"
            }

            else -> expense.type.orEmpty().ifBlank { "Não informado" }
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
                    description = expense.displayDescription,
                    amount = formattedAmount,
                    categoryName = categoryName,
                    typeLabel = typeLabel,
                    textColor = dialogTextColor
                )

                DetailsGroup(
                    paymentSplits = expense.payment_splits.orEmpty().filterNotNull().map {
                        it.copy(payment_source = it.payment_source.orEmpty().ifBlank { "Não informado" })
                    }.ifEmpty {
                        expense.payment_source?.let { source ->
                            listOf(PaymentSplit(payment_source = source, amount = expense.amount))
                        }.orEmpty()
                    },
                    date = formattedDate,
                    paidAtLabel = paidAtLabel,
                    textColor = dialogTextColor
                )

                if (!expense.type.equals("Fixa", ignoreCase = true)) {
                    AdvanceDetailsBlock(
                        isAdvanced = expense.isAdvanced,
                        advancedAtLabel = advancedAtLabel,
                        onAdvanceClick = onAdvanceClick,
                        onChangeDateClick = onChangeAdvanceDateClick,
                        onRemoveClick = onRemoveAdvanceClick
                    )
                }

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
private fun AdvanceDetailsBlock(
    isAdvanced: Boolean,
    advancedAtLabel: String?,
    onAdvanceClick: (() -> Unit)?,
    onChangeDateClick: (() -> Unit)?,
    onRemoveClick: (() -> Unit)?
) {
    if (!isAdvanced && onAdvanceClick == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrimaryBlue.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isAdvanced) {
            OutlinedButton(
                onClick = onAdvanceClick ?: {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Adiantar despesa", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        Text(
            text = advancedAtLabel?.let { "Adiantada em $it" } ?: "Despesa adiantada",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "O status Paga é independente deste adiantamento.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        if (onChangeDateClick != null || onRemoveClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onChangeDateClick?.let { onClick ->
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Alterar data", color = PrimaryBlue, maxLines = 1)
                    }
                }
                onRemoveClick?.let { onClick ->
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Remover", color = DangerRed, maxLines = 1)
                    }
                }
            }
        }
    }
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

        FinancialValue(
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
private fun DetailsGroup(
    paymentSplits: List<PaymentSplit>,
    date: String,
    paidAtLabel: String?,
    textColor: Color
) {
    val blockColor = MaterialTheme.colorScheme.expenseDetailBlock

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
        if (paymentSplits.size <= 1) {
            DetailLine(
                label = "Origem",
                value = paymentSplits.firstOrNull()?.payment_source ?: "Não informado",
                textColor = textColor
            )
        } else {
            Text(
                text = "Pagamento dividido",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            paymentSplits.forEach { split ->
                DetailLine(
                    label = split.payment_source,
                    value = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(split.amount),
                    sensitive = true,
                    textColor = textColor
                )
            }
        }
        HorizontalDivider(color = textColor.copy(alpha = 0.08f))
        DetailLine(label = "Data", value = date, textColor = textColor)
        paidAtLabel?.let { label ->
            HorizontalDivider(color = textColor.copy(alpha = 0.08f))
            DetailLine(label = "Pago em", value = label, textColor = textColor)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, textColor: Color, sensitive: Boolean = false) {
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
        if (sensitive) {
            FinancialValue(
                text = value,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.3f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
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
}

@Composable
private fun NotesDetailBox(value: String, textColor: Color) {
    val colorScheme = MaterialTheme.colorScheme
    val blockColor = colorScheme.expenseDetailBlock
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
            if (e is kotlinx.coroutines.CancellationException) throw e
        dateString
    }
}

private fun formatExpensePaidAt(dateString: String): String {
    val datePart = dateString.substringBefore('T').substringBefore(' ')
    val formattedDate = datePart.split('-').let { parts ->
        if (parts.size == 3 && parts.all { it.isNotBlank() }) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            datePart
        }
    }

    val time = Regex("[T ](\\d{2}:\\d{2})").find(dateString)?.groupValues?.get(1)
    return time?.let { "$formattedDate às $it" } ?: formattedDate
}

private fun formatExpenseAdvancedAt(dateString: String): String {
    val datePart = dateString.substringBefore('T').substringBefore(' ')
    val parts = datePart.split('-')
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else datePart
}
