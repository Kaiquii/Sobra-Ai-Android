package com.example.appfinanceiro.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.ExitConfirmationDialog
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.feature.home.components.AddExpenseButton
import com.example.appfinanceiro.feature.home.components.DespesasHeaderSection
import com.example.appfinanceiro.feature.home.components.ExpenseItem
import com.example.appfinanceiro.feature.home.components.FilterOptionItem
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.home.components.ResumoFinanceiroSection
import com.example.appfinanceiro.feature.home.utils.formatCurrency
import com.example.appfinanceiro.feature.home.utils.formatExpenseDate
import com.example.appfinanceiro.feature.home.utils.getCategoryIconAndColor
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()

    var currentMonthIndex by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    var showCategoryFilterModal by remember { mutableStateOf(false) }
    var showPaymentSourceFilterModal by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedPaymentSource by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    var refreshIncomeActions by remember { mutableIntStateOf(0) }
    val selectedCategoryName = selectedCategoryId?.let { uiState.categoriesMap[it] }
    val selectedPaymentSourceName = when (selectedPaymentSource) {
        "Salario" -> "Salário"
        "Adiantamento" -> "Adiantamento"
        "Renda Extra" -> "Renda Extra"
        else -> null
    }

    val filteredExpenses = uiState.expensesData.filter { expense ->
        val matchesCategory =
            selectedCategoryId == null || expense.category_id == selectedCategoryId

        val matchesPaymentSource = when (selectedPaymentSource) {
            null -> true

            "Salario" ->
                expense.payment_source.equals("Salario", ignoreCase = true) ||
                        expense.payment_source.equals("Salário", ignoreCase = true)

            "Adiantamento" ->
                expense.payment_source.equals("Adiantamento", ignoreCase = true)

            "Renda Extra" ->
                expense.payment_source.equals("Renda Extra", ignoreCase = true)

            else -> true
        }

        matchesCategory && matchesPaymentSource
    }

    val salarioAtual = uiState.incomesData.firstOrNull {
        (it.source.equals("Salario", ignoreCase = true) ||
                it.source.equals("Salário", ignoreCase = true)) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    }

    val adiantamentoAtual = uiState.incomesData.firstOrNull {
        it.source.equals("Adiantamento", ignoreCase = true) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    }

    val rendaExtraAtual = uiState.incomesData.firstOrNull {
        it.source.equals("Renda Extra", ignoreCase = true) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    }

    fun changeMonth(amount: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonthIndex)
            add(Calendar.MONTH, amount)
        }

        currentMonthIndex = cal.get(Calendar.MONTH)
        currentYear = cal.get(Calendar.YEAR)
    }

    LaunchedEffect(currentMonthIndex, currentYear, userToken, refreshIncomeActions) {
        userToken?.let { token ->
            viewModel.loadAll(token, currentMonthIndex + 1, currentYear)
        }
    }

    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            sessionManager.clearSession()
            viewModel.clearSessionExpired()
            onSessionExpired()
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Visão Mensal",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            StandardBottomBar(
                itemSelecionado = 0,
                onItemClick = onNavigate,
                onAddClick = onAddClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthSelector(
                    monthIndex = currentMonthIndex,
                    currentYear = currentYear,
                    onPrevClick = { changeMonth(-1) },
                    onNextClick = { changeMonth(1) }
                )
            }

            item {
                ResumoFinanceiroSection(
                    isLoading = uiState.isSummaryLoading || uiState.isIncomesLoading,
                    errorMessage = uiState.summaryError ?: uiState.incomesError,
                    onRetry = {
                        userToken?.let { token ->
                            viewModel.loadSummary(token, currentMonthIndex + 1, currentYear)
                            viewModel.loadIncomes(token, currentMonthIndex + 1, currentYear)
                        }
                    },
                    data = uiState.summaryData,
                    salarioIncome = salarioAtual,
                    adiantamentoIncome = adiantamentoAtual,
                    rendaExtraIncome = rendaExtraAtual,
                    token = userToken,
                    month = currentMonthIndex + 1,
                    year = currentYear,
                    onRefresh = { refreshIncomeActions++ }
                )
            }

            item {
                DespesasHeaderSection(
                    isLoading = uiState.isExpensesLoading,
                    errorMessage = uiState.expensesError,
                    onRetry = {
                        userToken?.let { token ->
                            viewModel.loadExpenses(token, currentMonthIndex + 1, currentYear)
                        }
                    },
                    expenses = filteredExpenses,
                    onCategoryFilterClick = { showCategoryFilterModal = true },
                    onPaymentSourceFilterClick = { showPaymentSourceFilterModal = true },
                    isCategoryFiltered = selectedCategoryId != null,
                    isPaymentSourceFiltered = selectedPaymentSource != null,
                    selectedCategoryName = selectedCategoryName,
                    selectedPaymentSourceName = selectedPaymentSourceName,
                    onClearCategoryFilter = { selectedCategoryId = null },
                    onClearPaymentSourceFilter = { selectedPaymentSource = null },
                    onAddClick = onAddClick
                )
            }

            items(
                items = filteredExpenses,
                key = { expense -> expense.id }
            ) { expense ->
                val categoryName = uiState.categoriesMap[expense.category_id] ?: "Outros"
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

            item {
                AddExpenseButton(onAddClick)
            }

        }
    }

    if (showCategoryFilterModal) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryFilterModal = false },
            containerColor = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Filtrar por Categoria",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FilterOptionItem(
                    label = "Todas",
                    isSelected = selectedCategoryId == null,
                    onClick = {
                        selectedCategoryId = null
                        showCategoryFilterModal = false
                    }
                )

                uiState.categoriesMap.forEach { (id, name) ->
                    FilterOptionItem(
                        label = name,
                        isSelected = selectedCategoryId == id,
                        onClick = {
                            selectedCategoryId = id
                            showCategoryFilterModal = false
                        }
                    )
                }
            }
        }
    }

    if (showPaymentSourceFilterModal) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSourceFilterModal = false },
            containerColor = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Filtrar por Origem",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FilterOptionItem(
                    label = "Todas",
                    isSelected = selectedPaymentSource == null,
                    onClick = {
                        selectedPaymentSource = null
                        showPaymentSourceFilterModal = false
                    }
                )

                FilterOptionItem(
                    label = "Salário",
                    isSelected = selectedPaymentSource == "Salario",
                    onClick = {
                        selectedPaymentSource = "Salario"
                        showPaymentSourceFilterModal = false
                    }
                )

                FilterOptionItem(
                    label = "Adiantamento",
                    isSelected = selectedPaymentSource == "Adiantamento",
                    onClick = {
                        selectedPaymentSource = "Adiantamento"
                        showPaymentSourceFilterModal = false
                    }
                )

                FilterOptionItem(
                    label = "Renda Extra",
                    isSelected = selectedPaymentSource == "Renda Extra",
                    onClick = {
                        selectedPaymentSource = "Renda Extra"
                        showPaymentSourceFilterModal = false
                    }
                )
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                coroutineScope.launch {
                    sessionManager.clearSession()
                    showExitDialog = false
                }
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }
}
