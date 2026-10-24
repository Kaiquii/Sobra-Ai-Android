package com.example.appfinanceiro.feature.home.components.income

import br.com.sobraai.ui.runtime.UiToast as Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Income
import kotlinx.coroutines.launch

data class IncomeDeleteState(
    val income: Income,
    val label: String
)

@Composable
fun IncomeDeleteDialog(
    state: IncomeDeleteState,
    token: String?,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    actionsViewModel: FinanceActionsViewModel = rememberPresentation("FinanceActionsViewModel") { services -> FinanceActionsViewModel(services.finance, services.files) }
) {
    val context = LocalUiServices.current
    val coroutineScope = rememberCoroutineScope()

    val dialogBackgroundColor = MaterialTheme.colorScheme.background
    val dialogTextColor = MaterialTheme.colorScheme.onBackground
    val dialogSecondaryTextColor = dialogTextColor.copy(alpha = 0.8f)

    var deleteFuture by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBackgroundColor,
        titleContentColor = dialogTextColor,
        textContentColor = dialogTextColor,
        title = {
            Text(
                text = "Excluir renda",
                color = dialogTextColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tem certeza que deseja remover ${state.label}?",
                    color = dialogTextColor
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteFuture,
                        onCheckedChange = { deleteFuture = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                    )
                    Text(
                        text = "Excluir esta e as próximas",
                        color = dialogSecondaryTextColor,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            actionsViewModel.deleteIncome(
                                token = token ?: return@launch,
                                id = state.income.id,
                                deleteFuture = if (deleteFuture) true else null
                            )

                            Toast.success(
                                context,
                                "${state.label} removido!",
                                Toast.LENGTH_SHORT
                            ).show()

                            onSuccess()
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            Toast.error(
                                context,
                                e.userMessageOr("Erro ao remover ${state.label}"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text("Excluir", color = DangerRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PrimaryBlue)
            }
        }
    )
}
