package com.example.appfinanceiro.feature.home.components.income

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.IncomeRequest
import com.example.appfinanceiro.core.network.IncomeUpdateRequest
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import kotlinx.coroutines.launch

data class IncomeEditorState(
    val title: String,
    val source: String,
    val existingIncome: Income? = null
)

@Composable
fun IncomeEditorDialog(
    state: IncomeEditorState,
    token: String?,
    month: Int,
    year: Int,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dialogBackgroundColor = MaterialTheme.colorScheme.background
    val dialogTextColor = MaterialTheme.colorScheme.onBackground
    val dialogSecondaryTextColor = dialogTextColor.copy(alpha = 0.8f)

    var amountText by remember(state) {
        mutableStateOf(state.existingIncome?.amount?.toString()?.replace(".", ",") ?: "")
    }
    var updateFuture by remember(state) { mutableStateOf(true) }

    val isRendaExtra = state.source.equals("Renda Extra", ignoreCase = true)
    val isCreating = state.existingIncome == null

    var repeatFuture by remember(state) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBackgroundColor,
        titleContentColor = dialogTextColor,
        textContentColor = dialogTextColor,
        title = {
            Text(
                text = if (state.existingIncome == null) {
                    "Criar ${state.title}"
                } else {
                    "Editar ${state.title}"
                },
                color = dialogTextColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.count { c -> c == ',' } <= 1 && it.all { c -> c.isDigit() || c == ',' }) {
                            amountText = it
                        }
                    },
                    label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isCreating && isRendaExtra) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = repeatFuture,
                            onCheckedChange = { repeatFuture = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                        )
                        Text(
                            text = "Repetir nos próximos meses",
                            color = dialogSecondaryTextColor,
                            fontSize = 13.sp
                        )
                    }
                }

                if (state.existingIncome != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = updateFuture,
                            onCheckedChange = { updateFuture = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                        )
                        Text(
                            text = "Atualizar este e os próximos meses",
                            color = dialogSecondaryTextColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.replace(",", ".").toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        Toast.makeText(context, "Informe um valor maior que zero", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    coroutineScope.launch {
                        try {
                            if (state.existingIncome == null) {
                                RetrofitClient.financeApi.createIncome(
                                    "Bearer $token",
                                    IncomeRequest(
                                        source = state.source,
                                        amount = amount,
                                        month = month,
                                        year = year,
                                        type = "Fixa",
                                        repeat_future = if (isRendaExtra) repeatFuture else null
                                    )
                                )

                                Toast.makeText(
                                    context,
                                    "${state.title} criado!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                RetrofitClient.financeApi.updateIncome(
                                    "Bearer $token",
                                    state.existingIncome.id,
                                    IncomeUpdateRequest(
                                        amount = amount,
                                        update_future = updateFuture
                                    )
                                )

                                Toast.makeText(
                                    context,
                                    "${state.title} atualizado!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            onSuccess()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Erro ao salvar ${state.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text("Salvar", color = PrimaryBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PrimaryBlue)
            }
        }
    )
}
