package com.example.appfinanceiro.feature.despesas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.components.AppDatePickerDialog
import com.example.appfinanceiro.core.designsystem.components.FinancialValue
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.displayDescription
import com.example.appfinanceiro.feature.despesas.monthName
import com.example.appfinanceiro.feature.despesas.scheduledMonthYear
import br.com.sobraai.ui.runtime.NumberFormat
import br.com.sobraai.ui.runtime.SimpleDateFormat
import br.com.sobraai.ui.runtime.Calendar
import br.com.sobraai.ui.runtime.Locale
import br.com.sobraai.ui.runtime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceExpenseDialog(
    expense: Expense,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val utc = remember { TimeZone.getTimeZone("UTC") }
    val todayMillis = remember { utcDayMillis(Calendar.getInstance()) }
    val scheduledMillis = remember(expense.date) { parseApiDateToUtcMillis(expense.date) }
    val maximumMillis = remember(scheduledMillis) {
        (scheduledMillis ?: Long.MAX_VALUE) - DAY_MILLIS
    }
    val initialMillis = remember(expense.advancedAt, maximumMillis) {
        parseApiDateToUtcMillis(expense.advancedAt)
            ?.coerceAtMost(maximumMillis)
            ?: todayMillis.coerceAtMost(maximumMillis)
    }
    val selectableDates = remember(maximumMillis) {
        object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= maximumMillis
        }
    }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = selectableDates
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedMillis = datePickerState.selectedDateMillis
    val selectedDateLabel = selectedMillis?.let(::formatUtcDateForDisplay) ?: "Selecionar data"
    val scheduledPeriod = expense.scheduledMonthYear()
        ?.let { (month, year) -> "${monthName(month)} de $year" }
        ?: "o mês previsto"
    val selectedPeriod = selectedMillis?.let(::monthYearFromUtcMillis)
    val selectedMonthLabel = selectedPeriod
        ?.let { (month, year) -> "${monthName(month)} de $year" }
        ?: "o mês escolhido"
    val formattedAmount = remember(expense.amount) {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(expense.amount)
    }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = if (expense.isAdvanced) "Alterar adiantamento" else "Adiantar despesa",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${expense.displayDescription} está prevista para $scheduledPeriod.",
                    color = MaterialTheme.colorScheme.onBackground
                )
                FinancialValue(
                    text = "$formattedAmount entrarão no cálculo de $selectedMonthLabel. A despesa continuará visível em $scheduledPeriod e não será descontada novamente nesse período.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Data do adiantamento",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isUpdating) { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedDateLabel, color = MaterialTheme.colorScheme.onSurface)
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Selecionar data do adiantamento",
                        tint = PrimaryBlue
                    )
                }
                Text(
                    text = "Esta ação não altera o status Paga.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Você pode escolher uma data futura, desde que seja anterior à data prevista da despesa.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedMillis?.let { onConfirm(formatUtcDateForApi(it)) }
                },
                enabled = selectedMillis != null && !isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(if (isUpdating) "Atualizando..." else "Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("Cancelar", color = PrimaryBlue)
            }
        }
    )

    if (showDatePicker) {
        AppDatePickerDialog(
            state = datePickerState,
            onConfirm = { selected ->
                if (selected != null && selected <= maximumMillis) {
                    datePickerState.selectedDateMillis = selected
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun RemoveAdvanceDialog(
    expense: Expense,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val formattedAmount = remember(expense.amount) {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(expense.amount)
    }
    val scheduledPeriod = expense.scheduledMonthYear()
        ?.let { (month, year) -> "${monthName(month)} de $year" }
        ?: "o período previsto"
    val advancedPeriod = parseApiDateToUtcMillis(expense.advancedAt)
        ?.let(::monthYearFromUtcMillis)
        ?.let { (month, year) -> "${monthName(month)} de $year" }
        ?: "o mês do adiantamento"

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                "Remover adiantamento?",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialValue(
                    "$formattedAmount deixarão de ser considerados em $advancedPeriod e voltarão para o cálculo de $scheduledPeriod.",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Esta ação não altera o status Paga.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                )
            ) {
                Text(if (isUpdating) "Removendo..." else "Remover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("Cancelar", color = PrimaryBlue)
            }
        }
    )
}

private const val DAY_MILLIS = 86_400_000L

private fun utcDayMillis(calendar: Calendar): Long {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    return utcCalendar.timeInMillis
}

private fun parseApiDateToUtcMillis(value: String?): Long? {
    val datePart = value?.substringBefore('T')?.substringBefore(' ') ?: return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(datePart)?.time
    }.getOrNull()
}

private fun formatUtcDateForDisplay(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(millis)

private fun formatUtcDateForApi(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(millis)

private fun monthYearFromUtcMillis(millis: Long): Pair<Int, Int> =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }.let {
        (it.get(Calendar.MONTH) + 1) to it.get(Calendar.YEAR)
    }
