package com.example.appfinanceiro.feature.perfil.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.network.Income
@Composable
fun IncomeCard(
    title: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    existingIncome: Income?,
    updateFuture: Boolean,
    onUpdateFutureChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    showRepeatFutureOption: Boolean = false,
    repeatFuture: Boolean = false,
    onRepeatFutureChange: (Boolean) -> Unit = {}
) {
    SettingsCard(compact = true, spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsIcon(when (title) {
                "Salário" -> Icons.Default.AccountBalanceWallet
                "Adiantamento" -> Icons.Default.Payments
                else -> Icons.Default.Savings
            }, compact = true)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        SettingsTextField(
            value = amount,
            onValueChange = {
                if (it.count { c -> c == ',' } <= 1 && it.all { c -> c.isDigit() || c == ',' }) {
                    onAmountChange(it)
                }
            },
            label = "Valor mensal",
            prefix = { Text("R$", modifier = Modifier.padding(end = 8.dp)) },
            placeholder = "0,00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (showRepeatFutureOption) {
            IncomeFutureOption("Repetir nos próximos meses", repeatFuture, onRepeatFutureChange)
        }
        if (existingIncome != null) {
            IncomeFutureOption("Atualizar este e os próximos meses", updateFuture, onUpdateFutureChange)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsPrimaryButton(
                label = if (existingIncome == null) "Cadastrar renda" else "Salvar alterações",
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
            if (existingIncome != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Excluir ${title.lowercase()}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeFutureOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            label,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
