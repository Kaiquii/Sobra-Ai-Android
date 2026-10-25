package com.example.appfinanceiro.feature.home

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.appfinanceiro.core.designsystem.components.AppSelectOption
import com.example.appfinanceiro.core.designsystem.components.AppSelectDialog

import com.example.appfinanceiro.core.network.canonicalPaymentSource
import com.example.appfinanceiro.core.network.PAYMENT_SOURCE_VALUES
import com.example.appfinanceiro.core.network.paymentSourceDisplayName
import com.example.appfinanceiro.core.network.incomeFor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.network.displayDescription
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyButton
import com.example.appfinanceiro.feature.home.utils.formatCurrency
import com.example.appfinanceiro.core.date.shiftMonth
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.ExitConfirmationDialog
import com.example.appfinanceiro.core.designsystem.components.ExpenseDetailsDialog
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.paymentCardSourceLabel
import com.example.appfinanceiro.core.network.paymentSources
import com.example.appfinanceiro.feature.home.components.AddExpenseButton
import com.example.appfinanceiro.feature.home.components.DespesasHeaderSection
import com.example.appfinanceiro.feature.home.components.ExpenseItem
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.home.components.ResumoFinanceiroSection
import com.example.appfinanceiro.feature.home.utils.formatExpenseDate
import com.example.appfinanceiro.feature.despesas.formatAdvancedDate
import kotlinx.coroutines.launch
import br.com.sobraai.ui.runtime.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onAssistantClick: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    viewModel: HomeViewModel = rememberPresentation("HomeViewModel") { services -> HomeViewModel(services.finance) }
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val context = LocalUiServices.current
    val coroutineScope = rememberCoroutineScope()

    val sessionManager = remember { context.session }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val financeRevision by FinanceDataChanges.revision.collectAsState()
    val listState = rememberLazyListState()
    val isLoadingMonth = uiState.isSummaryLoading ||
            uiState.isIncomesLoading ||
            uiState.isExpensesLoading
    val hasLoadedHome = uiState.hasLoadedSummary &&
            uiState.hasLoadedIncomes &&
            uiState.hasLoadedExpenses
    var currentMonthIndex by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    }
    var currentYear by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
    }

    var showCategoryFilterModal by remember { mutableStateOf(false) }
    var showPaymentSourceFilterModal by remember { mutableStateOf(false) }
    var showPaymentStatusFilterModal by remember { mutableStateOf(false) }
    var expenseToView by remember { mutableStateOf<Expense?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPaymentSource by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPaymentStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    var refreshIncomeActions by remember { mutableIntStateOf(0) }
    val selectedCategoryName = selectedCategoryId?.let { uiState.categoriesMap[it] }
    val selectedPaymentSourceName = selectedPaymentSource?.let(::paymentSourceDisplayName)
    val selectedPaymentStatusName = when (selectedPaymentStatus) {
        "paid" -> "Pagas"
        "pending" -> "Pendentes"
        else -> "Todas"
    }

    val filteredExpenses = uiState.expensesData.filter { expense ->
        val matchesCategory =
            selectedCategoryId == null || expense.category_id == selectedCategoryId

        val matchesPaymentSource = selectedPaymentSource == null || expense.paymentSources().any {
            canonicalPaymentSource(it) == canonicalPaymentSource(selectedPaymentSource!!)
        }

        matchesCategory && matchesPaymentSource
    }

    val salarioAtual = uiState.incomesData.firstOrNull {
        (it.source.equals("Salario", ignoreCase = true) ||
                it.source.equals("Salário", ignoreCase = true)) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    } ?: if (uiState.isIncomesLoading && uiState.hasLoadedIncomes) {
        uiState.incomesData.firstOrNull {
            it.source.equals("Salario", ignoreCase = true) ||
                    it.source.equals("Salário", ignoreCase = true)
        }
    } else null

    val adiantamentoAtual = uiState.incomesData.firstOrNull {
        it.source.equals("Adiantamento", ignoreCase = true) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    } ?: if (uiState.isIncomesLoading && uiState.hasLoadedIncomes) {
        uiState.incomesData.firstOrNull {
            it.source.equals("Adiantamento", ignoreCase = true)
        }
    } else null

    val rendaExtraAtual = uiState.incomesData.firstOrNull {
        it.source.equals("Renda Extra", ignoreCase = true) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    } ?: if (uiState.isIncomesLoading && uiState.hasLoadedIncomes) {
        uiState.incomesData.firstOrNull {
            it.source.equals("Renda Extra", ignoreCase = true)
        }
    } else null

    fun changeMonth(amount: Int) {
        val target = shiftMonth(currentMonthIndex, currentYear, amount)
        currentMonthIndex = target.monthIndex
        currentYear = target.year
    }

    LaunchedEffect(currentMonthIndex, currentYear, userToken, refreshIncomeActions, financeRevision) {
        userToken?.let { token ->
            viewModel.loadAll(
                token,
                currentMonthIndex + 1,
                currentYear,
                selectedPaymentStatus,
                refreshKey = listOf(financeRevision, refreshIncomeActions),
                reuseRecent = true
            )
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
        modifier = Modifier.swipeNavigation(0, onNavigate),
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
                actions = {
                    if (isLoadingMonth && hasLoadedHome) {
                        AppLoadingIndicator(modifier = Modifier.padding(end = 4.dp))
                    }
                    FinancialPrivacyButton()
                    IconButton(onClick = onAssistantClick) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Assistente IA",
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
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

            homeLoadNotices(uiState).forEach { notice ->
                item(key = "home_error_${notice.sources}") {
                    AppDataErrorBanner(
                        message = notice.message,
                        isRetrying = notice.retrying,
                        onRetry = {
                            userToken?.let { token ->
                                notice.sources.forEach { source ->
                                    when (source) {
                                        HomeLoadSource.Summary -> viewModel.loadSummary(token, currentMonthIndex + 1, currentYear)
                                        HomeLoadSource.Incomes -> viewModel.loadIncomes(token, currentMonthIndex + 1, currentYear)
                                        HomeLoadSource.Expenses -> viewModel.loadExpenses(token, currentMonthIndex + 1, currentYear, selectedPaymentStatus)
                                        HomeLoadSource.Categories -> if (HomeLoadSource.Expenses !in notice.sources)
                                            viewModel.loadCategories(token)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                ResumoFinanceiroSection(
                    isLoading = uiState.isSummaryLoading || uiState.isIncomesLoading,
                    errorMessage = uiState.summaryError ?: uiState.incomesError,
                    errorHandledExternally = true,
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
                    vaIncome = uiState.incomesData.incomeFor("VA", currentMonthIndex + 1, currentYear),
                    vrIncome = uiState.incomesData.incomeFor("VR", currentMonthIndex + 1, currentYear),
                    token = userToken,
                    month = currentMonthIndex + 1,
                    year = currentYear,
                    onRefresh = { refreshIncomeActions++ }
                )
            }

            item {
                DespesasHeaderSection(
                    isLoading = uiState.isExpensesLoading,
                    hasLoadedOnce = uiState.hasLoadedExpenses,
                    errorMessage = uiState.expensesError,
                    errorHandledExternally = true,
                    onRetry = {
                        userToken?.let { token ->
                            viewModel.loadExpenses(
                                token,
                                currentMonthIndex + 1,
                                currentYear,
                                selectedPaymentStatus
                            )
                        }
                    },
                    expenses = filteredExpenses,
                    onCategoryFilterClick = { showCategoryFilterModal = true },
                    onPaymentSourceFilterClick = { showPaymentSourceFilterModal = true },
                    onPaymentStatusFilterClick = { showPaymentStatusFilterModal = true },
                    isCategoryFiltered = selectedCategoryId != null,
                    isPaymentSourceFiltered = selectedPaymentSource != null,
                    isPaymentStatusFiltered = selectedPaymentStatus != null,
                    selectedCategoryName = selectedCategoryName,
                    selectedPaymentSourceName = selectedPaymentSourceName,
                    selectedPaymentStatusName = selectedPaymentStatusName,
                    onClearCategoryFilter = { selectedCategoryId = null },
                    onClearPaymentSourceFilter = { selectedPaymentSource = null },
                    onClearPaymentStatusFilter = {
                        selectedPaymentStatus = null
                        userToken?.let { token ->
                            viewModel.loadExpenses(token, currentMonthIndex + 1, currentYear)
                        }
                    },
                    onAddClick = onAddClick
                )
            }

            items(
                items = filteredExpenses,
                key = { expense -> expense.id }
            ) { expense ->
                val categoryName = uiState.categoriesMap[expense.category_id] ?: "Outros"
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
                    title = expense.displayDescription,
                    categoryName = categoryName,
                    paymentSource = expense.paymentCardSourceLabel(),
                    type = typeText,
                    date = formattedDate,
                    value = "- ${formatCurrency(expense.amount)}",
                    expenseType = expense.type,
                    isPaid = expense.is_paid,
                    isAdvanced = expense.isAdvanced,
                    advancedLabel = formatAdvancedDate(expense.advancedAt)?.let { "Adiantada em $it" },
                    onViewClick = { expenseToView = expense }
                )
            }

            item {
                AddExpenseButton(onAddClick)
            }

        }
    }

    if (showCategoryFilterModal) {
        AppSelectDialog(
            title = "Filtrar por categoria",
            options = listOf(AppSelectOption<Int?>(null, "Todas")) +
                uiState.categoriesMap.entries.sortedBy { it.value.lowercase() }.map { AppSelectOption<Int?>(it.key, it.value) },
            selected = selectedCategoryId,
            onDismiss = { showCategoryFilterModal = false },
            onSelect = { selectedCategoryId = it; showCategoryFilterModal = false }
        )
    }
    if (showPaymentSourceFilterModal) {
        AppSelectDialog(
            title = "Origem do pagamento",
            options = listOf(AppSelectOption<String?>(null, "Todas")) +
                PAYMENT_SOURCE_VALUES.map { AppSelectOption<String?>(it, paymentSourceDisplayName(it)) },
            selected = selectedPaymentSource?.let(::canonicalPaymentSource),
            onDismiss = { showPaymentSourceFilterModal = false },
            onSelect = { selectedPaymentSource = it; showPaymentSourceFilterModal = false }
        )
    }
    if (showPaymentStatusFilterModal) {
        AppSelectDialog(
            title = "Status do pagamento",
            options = listOf(AppSelectOption<String?>(null, "Todas"), AppSelectOption("pending", "Pendentes"), AppSelectOption("paid", "Pagas")),
            selected = selectedPaymentStatus,
            onDismiss = { showPaymentStatusFilterModal = false },
            onSelect = { status ->
                selectedPaymentStatus = status
                showPaymentStatusFilterModal = false
                userToken?.let { token -> viewModel.loadExpenses(token, currentMonthIndex + 1, currentYear, status) }
            }
        )
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

    expenseToView?.let { expense ->
        ExpenseDetailsDialog(
            expense = expense,
            categoryName = uiState.categoriesMap[expense.category_id] ?: "Outros",
            onDismiss = { expenseToView = null }
        )
    }
}
