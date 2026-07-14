package com.example.appfinanceiro.feature.perfil.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.IncomeRequest
import com.example.appfinanceiro.core.network.IncomeUpdateRequest
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import com.example.appfinanceiro.feature.home.components.MonthSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesRendaScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userToken by remember { SessionManager(context) }.token.collectAsState(initial = null)

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    val dialogSurfaceColor = MaterialTheme.colorScheme.background
    val dialogTextColor = MaterialTheme.colorScheme.onBackground
    val dialogSecondaryTextColor = dialogTextColor.copy(alpha = 0.8f)

    val calendar = remember { Calendar.getInstance() }
    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isDeleting by remember { mutableStateOf(false) }

    var salarioAmount by remember { mutableStateOf("") }
    var adiantamentoAmount by remember { mutableStateOf("") }
    var rendaExtraAmount by remember { mutableStateOf("") }

    var salarioUpdateFuture by remember { mutableStateOf(true) }
    var adiantamentoUpdateFuture by remember { mutableStateOf(true) }
    var rendaExtraUpdateFuture by remember { mutableStateOf(true) }
    var rendaExtraRepeatFuture by remember { mutableStateOf(false) }

    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    var incomeDeleteLabel by remember { mutableStateOf("") }
    var deleteFutureSelected by remember { mutableStateOf(false) }

    val selectedMonth = selectedMonthIndex + 1

    LaunchedEffect(userToken, refreshTrigger) {
        if (userToken != null) {
            isLoading = true
            try {
                val incomesResponse = RetrofitClient.financeApi.getIncomes("Bearer $userToken")
                incomes = incomesResponse.incomes
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar configurações", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
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


    LaunchedEffect(salarioAtual, adiantamentoAtual, rendaExtraAtual) {
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
                                val response = RetrofitClient.financeApi.deleteIncome(
                                    token = "Bearer $userToken",
                                    id = selectedIncome.id,
                                    deleteFuture = if (deleteFutureSelected) true else null
                                )

                                Toast.makeText(
                                    context,
                                    response.message.ifBlank { "$incomeDeleteLabel removido!" },
                                    Toast.LENGTH_SHORT
                                ).show()

                                incomeToDelete = null
                                deleteFutureSelected = false
                                refreshTrigger++
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Erro ao remover $incomeDeleteLabel",
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
            TopAppBar(
                title = {
                    Text(
                        "Configurações de Renda",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        "Rendas Fixas",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MonthSelector(
                        monthIndex = selectedMonthIndex,
                        currentYear = selectedYear,
                        onPrevClick = {
                            if (selectedMonthIndex == 0) {
                                selectedMonthIndex = 11
                                selectedYear--
                            } else {
                                selectedMonthIndex--
                            }
                        },
                        onNextClick = {
                            if (selectedMonthIndex == 11) {
                                selectedMonthIndex = 0
                                selectedYear++
                            } else {
                                selectedMonthIndex++
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Referência selecionada: ${selectedMonth.toString().padStart(2, '0')}/$selectedYear",
                        color = TextMuted,
                        fontSize = 13.sp
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
                        surfaceColor = surfaceColor
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
                        surfaceColor = surfaceColor
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
                        surfaceColor = surfaceColor
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
        Toast.makeText(context, "Informe um valor maior que zero", Toast.LENGTH_SHORT).show()
        return
    }

    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        try {
            if (existingIncome == null) {
                val isRendaExtra = source.equals("Renda Extra", ignoreCase = true)

                RetrofitClient.financeApi.createIncome(
                    "Bearer $token",
                    IncomeRequest(
                        source = source,
                        amount = amount,
                        month = month,
                        year = year,
                        type = "Fixa",
                        repeat_future = if (isRendaExtra) repeatFuture else null
                    )
                )

                Toast.makeText(context, "$source cadastrado!", Toast.LENGTH_SHORT).show()
            }
            else {
                RetrofitClient.financeApi.updateIncome(
                    "Bearer $token",
                    existingIncome.id,
                    IncomeUpdateRequest(
                        amount = amount,
                        update_future = updateFuture
                    )
                )
                Toast.makeText(context, "$source atualizado!", Toast.LENGTH_SHORT).show()
            }
            refresh()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao salvar $source", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun IncomeCard(
    title: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    existingIncome: Income?,
    updateFuture: Boolean,
    onUpdateFutureChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    surfaceColor: Color,
    showRepeatFutureOption: Boolean = false,
    repeatFuture: Boolean = false,
    onRepeatFutureChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        OutlinedTextField(
            value = amount,
            onValueChange = {
                if (it.count { c -> c == ',' } <= 1 && it.all { c -> c.isDigit() || c == ',' }) {
                    onAmountChange(it)
                }
            },
            label = { Text("Valor") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (showRepeatFutureOption) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = repeatFuture,
                    onCheckedChange = onRepeatFutureChange
                )
                Text(
                    "Repetir nos próximos meses",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }

        if (existingIncome != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = updateFuture,
                    onCheckedChange = onUpdateFutureChange
                )
                Text(
                    "Atualizar este e os próximos meses",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (existingIncome == null) "Cadastrar" else "Atualizar")
            }

            if (existingIncome != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Excluir")
                }
            }
        }
    }
}
