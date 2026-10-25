package com.example.appfinanceiro.feature.perfil.components

import br.com.sobraai.ui.runtime.UiServices as Context
import br.com.sobraai.ui.runtime.UiToast as Toast
import com.example.appfinanceiro.feature.perfil.SettingsListLoadViewModel
import com.example.appfinanceiro.core.data.FinanceDataChanges
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.date.shiftMonth
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.incomeFor
import com.example.appfinanceiro.core.network.IncomeRequest
import com.example.appfinanceiro.core.network.IncomeUpdateRequest
import com.example.appfinanceiro.feature.home.components.MonthSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import br.com.sobraai.ui.runtime.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesRendaScreen(
    onNavigateBack: () -> Unit,
    actionsViewModel: FinanceActionsViewModel = rememberPresentation("FinanceActionsViewModel") { services -> FinanceActionsViewModel(services.finance, services.files) }
) {
    val context = LocalUiServices.current
    val coroutineScope = rememberCoroutineScope()
    val userToken by remember { context.session }.token.collectAsState(initial = null)

    val backgroundColor = MaterialTheme.colorScheme.background

    val dialogSurfaceColor = MaterialTheme.colorScheme.background
    val dialogTextColor = MaterialTheme.colorScheme.onBackground
    val dialogSecondaryTextColor = dialogTextColor.copy(alpha = 0.8f)

    val calendar = remember { Calendar.getInstance() }
    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    fun changeMonth(amount: Int) {
        val target = shiftMonth(selectedMonthIndex, selectedYear, amount)
        selectedMonthIndex = target.monthIndex
        selectedYear = target.year
    }

    val listModel = rememberPresentation("SettingsListLoadViewModel") { SettingsListLoadViewModel<Income>() }
    val listState by listModel.state.collectAsState()
    val incomes = listState.items
    val isLoading = listState.loading
    val hasLoadedOnce = listState.hasLoadedOnce
    val loadErrorMessage = listState.error
    val financeRevision by FinanceDataChanges.revision.collectAsState()
    var refreshTrigger by rememberSaveable { mutableIntStateOf(0) }
    var isDeleting by remember { mutableStateOf(false) }

    var salarioAmount by remember { mutableStateOf("") }
    var adiantamentoAmount by remember { mutableStateOf("") }
    var vaAmount by remember { mutableStateOf("") }
    var vrAmount by remember { mutableStateOf("") }
    var vaUpdateFuture by remember { mutableStateOf(true) }
    var vrUpdateFuture by remember { mutableStateOf(true) }

    var rendaExtraAmount by remember { mutableStateOf("") }

    var salarioUpdateFuture by remember { mutableStateOf(true) }
    var adiantamentoUpdateFuture by remember { mutableStateOf(true) }
    var rendaExtraUpdateFuture by remember { mutableStateOf(true) }
    var rendaExtraRepeatFuture by remember { mutableStateOf(false) }

    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    var incomeDeleteLabel by remember { mutableStateOf("") }
    var deleteFutureSelected by remember { mutableStateOf(false) }

    val selectedMonth = selectedMonthIndex + 1

    LaunchedEffect(userToken, refreshTrigger, financeRevision) {
        userToken?.let { token ->
            listModel.load(token, listOf(financeRevision, refreshTrigger), "Erro ao carregar configurações de renda") {
                actionsViewModel.getIncomes(token).incomes
            }
        }
    }

    val salarioAtual = currentIncome(
        incomes = incomes,
        sources = listOf("Salario", "Salário"),
        month = selectedMonth,
        year = selectedYear
    )

    val adiantamentoAtual = currentIncome(
        incomes = incomes,
        sources = listOf("Adiantamento"),
        month = selectedMonth,
        year = selectedYear
    )

    val rendaExtraAtual = currentIncome(
        incomes = incomes,
        sources = listOf("Renda Extra"),
        month = selectedMonth,
        year = selectedYear
    )


    val vaAtual = incomes.incomeFor("VA", selectedMonth, selectedYear)
    val vrAtual = incomes.incomeFor("VR", selectedMonth, selectedYear)

    LaunchedEffect(salarioAtual, adiantamentoAtual, rendaExtraAtual, vaAtual, vrAtual, selectedMonth, selectedYear) {
        vaAmount = vaAtual?.amount?.toString()?.replace(".", ",") ?: ""
        vrAmount = vrAtual?.amount?.toString()?.replace(".", ",") ?: ""
        salarioAmount = salarioAtual?.amount?.toString()?.replace(".", ",") ?: ""
        adiantamentoAmount = adiantamentoAtual?.amount?.toString()?.replace(".", ",") ?: ""
        rendaExtraAmount = rendaExtraAtual?.amount?.toString()?.replace(".", ",") ?: ""
    }

    if (incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    incomeToDelete = null
                    deleteFutureSelected = false
                }
            },
            containerColor = dialogSurfaceColor,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Excluir renda",
                    color = dialogTextColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tem certeza que deseja remover $incomeDeleteLabel?",
                        color = dialogTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            deleteFutureSelected = !deleteFutureSelected
                        }
                    ) {
                        Checkbox(
                            checked = deleteFutureSelected,
                            onCheckedChange = { deleteFutureSelected = it },
                            enabled = !isDeleting,
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
                    enabled = !isDeleting,
                    onClick = {
                        val selectedIncome = incomeToDelete ?: return@TextButton

                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val response = actionsViewModel.deleteIncome(
                                    token = userToken ?: return@launch,
                                    id = selectedIncome.id,
                                    deleteFuture = if (deleteFutureSelected) true else null
                                )

                                Toast.success(
                                    context,
                                    response.message.ifBlank { "$incomeDeleteLabel removido!" },
                                    Toast.LENGTH_SHORT
                                ).show()

                                incomeToDelete = null
                                deleteFutureSelected = false
                                refreshTrigger++
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                Toast.error(
                                    context,
                                    e.userMessageOr("Erro ao remover $incomeDeleteLabel"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isDeleting = false
                            }
                        }
                    }
                ) {
                    Text(
                        "Confirmar",
                        color = DangerRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        incomeToDelete = null
                        deleteFutureSelected = false
                    }
                ) {
                    Text(
                        "Cancelar",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            SettingsTopBar(
                title = "Configurações de Renda",
                onBack = onNavigateBack,
                refreshing = isLoading && hasLoadedOnce
            )
        }
    ) { paddingValues ->
        if (isLoading && !hasLoadedOnce) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator(size = 40.dp, strokeWidth = 4.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                loadErrorMessage?.let { error ->
                    item(key = "income_settings_error") {
                        AppDataErrorBanner(
                            message = dataRequestErrorMessage(
                                errorMessage = error,
                                showingPreviousData = incomes.isNotEmpty(),
                                dataLabel = "as configurações de renda"
                            ),
                            isRetrying = isLoading,
                            onRetry = { refreshTrigger++ }
                        )
                    }
                }

                item {
                    MonthSelector(
                            monthIndex = selectedMonthIndex,
                            currentYear = selectedYear,
                            onPrevClick = { changeMonth(-1) },
                            onNextClick = { changeMonth(1) }
                    )
                }
                item {
                    IncomeCard(
                        title = "Salário",
                        amount = salarioAmount,
                        onAmountChange = { salarioAmount = it },
                        existingIncome = salarioAtual,
                        updateFuture = salarioUpdateFuture,
                        onUpdateFutureChange = { salarioUpdateFuture = it },
                        onSave = {
                            saveIncome(
                                actionsViewModel = actionsViewModel,
                                context = context,
                                source = "Salario",
                                amountText = salarioAmount,
                                existingIncome = salarioAtual,
                                updateFuture = salarioUpdateFuture,
                                month = selectedMonth,
                                year = selectedYear,
                                token = userToken,
                                refresh = { refreshTrigger++ }
                            )
                        },
                        onDelete = {
                            salarioAtual?.let {
                                incomeToDelete = it
                                incomeDeleteLabel = "o salário"
                                deleteFutureSelected = false
                            }
                        },
                    )
                }

                item {
                    IncomeCard(
                        title = "Adiantamento",
                        amount = adiantamentoAmount,
                        onAmountChange = { adiantamentoAmount = it },
                        existingIncome = adiantamentoAtual,
                        updateFuture = adiantamentoUpdateFuture,
                        onUpdateFutureChange = { adiantamentoUpdateFuture = it },
                        onSave = {
                            saveIncome(
                                actionsViewModel = actionsViewModel,
                                context = context,
                                source = "Adiantamento",
                                amountText = adiantamentoAmount,
                                existingIncome = adiantamentoAtual,
                                updateFuture = adiantamentoUpdateFuture,
                                month = selectedMonth,
                                year = selectedYear,
                                token = userToken,
                                refresh = { refreshTrigger++ }
                            )
                        },
                        onDelete = {
                            adiantamentoAtual?.let {
                                incomeToDelete = it
                                incomeDeleteLabel = "o adiantamento"
                                deleteFutureSelected = false
                            }
                        },
                    )
                }

                item {
                    IncomeCard(
                        title = "Vale-alimentação (VA)",
                        amount = vaAmount,
                        onAmountChange = { vaAmount = it },
                        existingIncome = vaAtual,
                        updateFuture = vaUpdateFuture,
                        onUpdateFutureChange = { vaUpdateFuture = it },
                        onSave = {
                            saveIncome(
                                actionsViewModel = actionsViewModel,
                                context = context,
                                source = "VA",
                                amountText = vaAmount,
                                existingIncome = vaAtual,
                                updateFuture = vaUpdateFuture,
                                month = selectedMonth,
                                year = selectedYear,
                                token = userToken,
                                refresh = { refreshTrigger++ }
                            )
                        },
                        onDelete = {
                            vaAtual?.let {
                                incomeToDelete = it
                                incomeDeleteLabel = "o vale-alimentação (VA)"
                                deleteFutureSelected = false
                            }
                        },
                    )
                }

                item {
                    IncomeCard(
                        title = "Vale-refeição (VR)",
                        amount = vrAmount,
                        onAmountChange = { vrAmount = it },
                        existingIncome = vrAtual,
                        updateFuture = vrUpdateFuture,
                        onUpdateFutureChange = { vrUpdateFuture = it },
                        onSave = {
                            saveIncome(
                                actionsViewModel = actionsViewModel,
                                context = context,
                                source = "VR",
                                amountText = vrAmount,
                                existingIncome = vrAtual,
                                updateFuture = vrUpdateFuture,
                                month = selectedMonth,
                                year = selectedYear,
                                token = userToken,
                                refresh = { refreshTrigger++ }
                            )
                        },
                        onDelete = {
                            vrAtual?.let {
                                incomeToDelete = it
                                incomeDeleteLabel = "o vale-refeição (VR)"
                                deleteFutureSelected = false
                            }
                        },
                    )
                }

                item {
                    IncomeCard(
                        title = "Renda Extra",
                        amount = rendaExtraAmount,
                        onAmountChange = { rendaExtraAmount = it },
                        existingIncome = rendaExtraAtual,
                        updateFuture = rendaExtraUpdateFuture,
                        onUpdateFutureChange = { rendaExtraUpdateFuture = it },
                        showRepeatFutureOption = rendaExtraAtual == null,
                        repeatFuture = rendaExtraRepeatFuture,
                        onRepeatFutureChange = { rendaExtraRepeatFuture = it },
                        onSave = {
                            saveIncome(
                                actionsViewModel = actionsViewModel,
                                context = context,
                                source = "Renda Extra",
                                amountText = rendaExtraAmount,
                                existingIncome = rendaExtraAtual,
                                updateFuture = rendaExtraUpdateFuture,
                                repeatFuture = rendaExtraRepeatFuture,
                                month = selectedMonth,
                                year = selectedYear,
                                token = userToken,
                                refresh = { refreshTrigger++ }
                            )
                        },
                        onDelete = {
                            rendaExtraAtual?.let {
                                incomeToDelete = it
                                incomeDeleteLabel = "a renda extra"
                                deleteFutureSelected = false
                            }
                        },
                    )

                }
            }
        }
    }
}

private fun currentIncome(
    incomes: List<Income>,
    sources: List<String>,
    month: Int,
    year: Int
): Income? {
    return incomes.firstOrNull { income ->
        sources.any { source ->
            income.source.equals(source, ignoreCase = true)
        } &&
                income.month == month &&
                income.year == year
    }
}


private fun saveIncome(
    actionsViewModel: FinanceActionsViewModel,
    context: Context,
    source: String,
    amountText: String,
    existingIncome: Income?,
    updateFuture: Boolean,
    repeatFuture: Boolean = false,
    month: Int,
    year: Int,
    token: String?,
    refresh: () -> Unit
) {
    val amount = amountText.replace(",", ".").toDoubleOrNull()
    if (amount == null || amount <= 0.0) {
        Toast.warning(context, "Informe um valor maior que zero", Toast.LENGTH_SHORT).show()
        return
    }

    val scope = actionsViewModel.viewModelScope
    scope.launch {
        try {
            if (existingIncome == null) {
                val isRendaExtra = source.equals("Renda Extra", ignoreCase = true)

                actionsViewModel.createIncome(
                    token ?: return@launch,
                    IncomeRequest(
                        source = source,
                        amount = amount,
                        month = month,
                        year = year,
                        type = "Fixa",
                        repeat_future = if (isRendaExtra) repeatFuture else null
                    )
                )

                Toast.success(context, "$source cadastrado!", Toast.LENGTH_SHORT).show()
            }
            else {
                actionsViewModel.updateIncome(
                    token ?: return@launch,
                    existingIncome.id,
                    IncomeUpdateRequest(
                        amount = amount,
                        update_future = updateFuture
                    )
                )
                Toast.success(context, "$source atualizado!", Toast.LENGTH_SHORT).show()
            }
            refresh()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Toast.error(
                context,
                e.userMessageOr("Erro ao salvar $source"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
