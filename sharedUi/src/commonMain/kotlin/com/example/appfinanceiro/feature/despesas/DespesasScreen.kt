package com.example.appfinanceiro.feature.despesas

import com.example.appfinanceiro.core.network.supportsAdvance

import br.com.sobraai.ui.runtime.UiToast as Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.network.displayDescription
import com.example.appfinanceiro.core.network.paymentSources
import com.example.appfinanceiro.core.date.shiftMonth
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyButton
import com.example.appfinanceiro.core.designsystem.components.FinancialValue
import com.example.appfinanceiro.core.designsystem.components.ExpenseDetailsDialog
import com.example.appfinanceiro.core.designsystem.components.ExpenseCard
import com.example.appfinanceiro.core.designsystem.components.ExpenseCardStyle
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.paymentCardSourceLabel
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.despesas.components.ExpenseListControls
import com.example.appfinanceiro.feature.despesas.components.ExpenseFiltersDialog
import com.example.appfinanceiro.feature.despesas.components.AdvanceExpenseDialog
import com.example.appfinanceiro.feature.despesas.components.RemoveAdvanceDialog
import br.com.sobraai.ui.runtime.NumberFormat
import br.com.sobraai.ui.runtime.SimpleDateFormat
import br.com.sobraai.ui.runtime.Calendar
import br.com.sobraai.ui.runtime.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespesasScreen(
    onNavigate: (Int) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onSessionExpired: () -> Unit = {},
    viewModel: DespesasViewModel = rememberPresentation("DespesasViewModel") { services -> DespesasViewModel(services.finance) }
) {
    val context = LocalUiServices.current
    val focusManager = LocalFocusManager.current
    val sessionManager = remember { context.session }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val financeRevision by FinanceDataChanges.revision.collectAsState()
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = colorScheme.background
    val textColor = colorScheme.onBackground
    val secondaryTextColor = colorScheme.onSurfaceVariant

    var refreshTrigger by remember { mutableIntStateOf(0) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("Todas") }
    var selectedPaymentStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPaymentSource by rememberSaveable { mutableStateOf<String?>(null) }
    var showFiltersDialog by rememberSaveable { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    var currentMonthIndex by rememberSaveable {
        mutableIntStateOf(calendar.get(Calendar.MONTH))
    }
    var currentYear by rememberSaveable {
        mutableIntStateOf(calendar.get(Calendar.YEAR))
    }

    fun changeMonth(amount: Int) {
        val target = shiftMonth(currentMonthIndex, currentYear, amount)
        currentMonthIndex = target.monthIndex
        currentYear = target.year
    }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToView by remember { mutableStateOf<Expense?>(null) }
    var expensePaymentStatusToChange by remember { mutableStateOf<Expense?>(null) }
    var expenseToAdvance by remember { mutableStateOf<Expense?>(null) }
    var expenseToRemoveAdvance by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(currentMonthIndex, currentYear, userToken, refreshTrigger, financeRevision) {
        userToken?.let { token ->
            viewModel.loadExpenses(
                token,
                currentMonthIndex + 1,
                currentYear,
                refreshKey = listOf(financeRevision, refreshTrigger)
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

    LaunchedEffect(
        uiState.deleteSuccessMessage,
        uiState.deleteErrorMessage,
        uiState.paymentStatusSuccessMessage,
        uiState.paymentStatusErrorMessage,
        uiState.advanceStatusSuccessMessage,
        uiState.advanceStatusErrorMessage
    ) {
        uiState.deleteSuccessMessage?.let {
            Toast.success(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.deleteErrorMessage?.let {
            Toast.error(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.paymentStatusSuccessMessage?.let {
            Toast.success(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.paymentStatusErrorMessage?.let {
            Toast.error(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.advanceStatusSuccessMessage?.let {
            Toast.success(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.advanceStatusErrorMessage?.let {
            Toast.error(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    val expenseFilters = listOf("Todas", "Parceladas", "Únicas", "Fixas")
    val modalFilteredExpenses = filterExpenses(
        uiState.expensesData, "", "Todas",
        selectedCategoryId, selectedPaymentSource, selectedPaymentStatus
    )
    val expenseCountsByFilter = expenseCountsByFilter(
        expenses = modalFilteredExpenses,
        searchQuery = searchQuery,
        filters = expenseFilters
    )
    val filteredExpenses = filterExpenses(
        expenses = modalFilteredExpenses,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter
    )
    val incomingAdvanced = incomingAdvancedExpenses(
        effectiveExpenses = uiState.effectiveExpensesData,
        selectedMonth = currentMonthIndex + 1,
        selectedYear = currentYear
    )
    val filteredIncomingAdvanced = filterExpenses(
        expenses = incomingAdvanced,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        categoryId = selectedCategoryId,
        paymentSource = selectedPaymentSource,
        paymentStatus = selectedPaymentStatus
    )
    val selectedFilterCount = expenseCountsByFilter[selectedFilter] ?: filteredExpenses.size
    val selectedFilterTotal = filteredExpenseTotal(
        effectiveExpenses = uiState.effectiveExpensesData,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        categoryId = selectedCategoryId,
        paymentSource = selectedPaymentSource,
        paymentStatus = selectedPaymentStatus
    )
    val selectedFilterTotalLabel = when (selectedFilter) {
        "Parceladas" -> "Total em Parceladas"
        "Únicas" -> "Total em Únicas"
        "Fixas" -> "Total em Fixas"
        else -> "Total das despesas"
    }
    val formattedSelectedFilterTotal = remember(selectedFilterTotal) {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
            .format(selectedFilterTotal)
    }

    Scaffold(
        modifier = Modifier.swipeNavigation(1, onNavigate),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Despesas Mensais",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(0) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            tint = textColor,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    if (uiState.isLoading && uiState.hasLoadedOnce) {
                        AppLoadingIndicator(modifier = Modifier.padding(end = 4.dp))
                    }
                    FinancialPrivacyButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            StandardBottomBar(
                itemSelecionado = 1,
                onItemClick = onNavigate,
                onAddClick = onAddClick
            )
        }
    ) { paddingValues ->
        // The header scrolls with the list so large fonts and the keyboard cannot hide the cards.
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "expenses_controls") {
                val summaryAvailable = uiState.hasLoadedOnce &&
                    (uiState.errorMessage == null || uiState.expensesData.isNotEmpty() ||
                        uiState.effectiveExpensesData.isNotEmpty())
                ExpenseListControls(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilter = selectedFilter,
                    onFilterSelect = { selectedFilter = it },
                    filtersActive = selectedCategoryId != null || selectedPaymentSource != null || selectedPaymentStatus != null,
                    onShowFilters = { focusManager.clearFocus(); showFiltersDialog = true },
                    currentMonthIndex = currentMonthIndex,
                    currentYear = currentYear,
                    count = selectedFilterCount.takeIf { summaryAvailable },
                    formattedTotal = formattedSelectedFilterTotal.takeIf { summaryAvailable },
                    totalLabel = selectedFilterTotalLabel,
                    onPrevClick = { changeMonth(-1) },
                    onNextClick = { changeMonth(1) }
                )
            }
            expensesLoadNotices(uiState).forEach { notice ->
                item(key = "load_error_${notice.target}") {
                    AppDataErrorBanner(
                        message = notice.message,
                        isRetrying = notice.retrying,
                        onRetry = {
                            userToken?.let { token ->
                                when (notice.target) {
                                    ExpensesRetryTarget.Categories -> viewModel.loadCategories(token)
                                    ExpensesRetryTarget.Expenses, ExpensesRetryTarget.Both ->
                                        viewModel.loadExpenses(token, currentMonthIndex + 1, currentYear)
                                }
                            }
                        }
                    )
                }
            }
            when {
                uiState.isLoading && !uiState.hasLoadedOnce -> item(key = "expenses_loading") {
                    Box(Modifier.fillParentMaxHeight(0.35f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AppLoadingIndicator(size = 40.dp, strokeWidth = 4.dp)
                    }
                }
                uiState.errorMessage != null && uiState.expensesData.isEmpty() -> Unit
                filteredExpenses.isEmpty() && filteredIncomingAdvanced.isEmpty() -> item(key = "expenses_empty") {
                    Box(Modifier.fillParentMaxHeight(0.35f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhuma despesa encontrada.", color = secondaryTextColor)
                    }
                }
                else -> {
                    if (filteredExpenses.isNotEmpty()) {
                        item(key = "expenses_summary") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Despesas previstas para ${monthName(currentMonthIndex + 1)}",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (selectedFilterCount == 1) "1 despesa" else "$selectedFilterCount despesas",
                                    color = secondaryTextColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    items(items = filteredExpenses, key = { expense -> "scheduled_${expense.id}" }) { expense ->
                        DespesaListItem(
                            expense = expense,
                            categoriesMap = uiState.categoriesMap,
                            onView = { expenseToView = expense },
                            onEdit = { onEditClick(expense.id) },
                            onDelete = { expenseToDelete = expense },
                            onPaymentStatusClick = { expensePaymentStatusToChange = expense },
                            onAdvanceClick = if (expense.supportsAdvance) ({ expenseToAdvance = expense }) else null
                        )
                    }
                    if (filteredIncomingAdvanced.isNotEmpty()) {
                        item(key = "incoming_advanced_header") {
                            AdvancedExpensesImpactNotice(
                                selectedMonth = currentMonthIndex + 1,
                                selectedYear = currentYear
                            )
                        }
                        items(items = filteredIncomingAdvanced, key = { expense -> "incoming_${expense.id}" }) { expense ->
                            AdvancedExpenseImpactCard(
                                expense = expense,
                                categoriesMap = uiState.categoriesMap,
                                onView = { expenseToView = expense },
                                onGoToOriginalExpense = {
                                    val scheduledPeriod = expense.scheduledMonthYear()
                                    if (scheduledPeriod == null) {
                                        Toast.error(context, "Não foi possível identificar o mês original desta despesa.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val (month, year) = scheduledPeriod
                                        searchQuery = ""
                                        selectedFilter = "Todas"
                                        selectedPaymentStatus = null
                                        selectedCategoryId = null
                                        selectedPaymentSource = null
                                        currentMonthIndex = month - 1
                                        currentYear = year
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        val isInstallmentExpense =
            expenseToDelete?.type?.equals("Parcelada", ignoreCase = true) == true
        val isFixedExpense =
            expenseToDelete?.type?.equals("Fixa", ignoreCase = true) == true

        var deleteFutureSelected by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) expenseToDelete = null },
            containerColor = backgroundColor,
            titleContentColor = textColor,
            textContentColor = textColor,
            title = {
                Text(
                    "Excluir Despesa",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Tem certeza que deseja excluir '${expenseToDelete?.displayDescription}'?",
                        color = textColor
                    )

                    if (isFixedExpense) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Esta exclusão removerá esta despesa no mês atual e também nos próximos meses.",
                            color = secondaryTextColor,
                            fontSize = 14.sp
                        )
                    } else if (isInstallmentExpense) {
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
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryBlue,
                                    uncheckedColor = secondaryTextColor,
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                "Excluir esta e todas as futuras",
                                color = textColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val token = userToken ?: return@TextButton
                        val selectedExpense = expenseToDelete ?: return@TextButton

                        viewModel.deleteExpense(
                            token = token,
                            expenseId = selectedExpense.id,
                            deleteFuture = if (isInstallmentExpense && deleteFutureSelected) true else null,
                            onDeleted = {
                                expenseToDelete = null
                                refreshTrigger++
                            }
                        )
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        "Confirmar",
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expenseToDelete = null },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Cancelar", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    expensePaymentStatusToChange?.let { expense ->
        val markingAsPaid = !expense.is_paid
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isUpdatingPaymentStatus) expensePaymentStatusToChange = null
            },
            containerColor = backgroundColor,
            title = {
                Text(
                    text = if (markingAsPaid) "Marcar como paga?" else "Desmarcar como paga?",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (markingAsPaid) {
                        "Confirma que \"${expense.displayDescription}\" foi paga?"
                    } else {
                        "Confirma que deseja desmarcar \"${expense.displayDescription}\" como paga?"
                    },
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val token = userToken ?: return@TextButton
                        viewModel.updateExpensePaymentStatus(
                            token = token,
                            expense = expense,
                            isPaid = markingAsPaid,
                            onUpdated = { expensePaymentStatusToChange = null }
                        )
                    },
                    enabled = !uiState.isUpdatingPaymentStatus
                ) {
                    Text(
                        text = if (uiState.isUpdatingPaymentStatus) {
                            "Atualizando..."
                        } else if (markingAsPaid) {
                            "Marcar como paga"
                        } else {
                            "Desmarcar"
                        },
                        color = if (markingAsPaid) GreenPositive else PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expensePaymentStatusToChange = null },
                    enabled = !uiState.isUpdatingPaymentStatus
                ) {
                    Text("Cancelar", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showFiltersDialog) {
        ExpenseFiltersDialog(
            categories = uiState.categoriesMap,
            categoryOptions = expenseCategoryOptions(
                categories = uiState.categoriesMap,
                expenses = uiState.expensesData + incomingAdvanced
            ),
            categoryId = selectedCategoryId,
            paymentSource = selectedPaymentSource,
            paymentStatus = selectedPaymentStatus,
            onDismiss = { showFiltersDialog = false },
            onApply = { category, source, status ->
                selectedCategoryId = category
                selectedPaymentSource = source
                selectedPaymentStatus = status
                showFiltersDialog = false
            }
        )
    }

    expenseToView?.let { expense ->
        ExpenseDetailsDialog(
            expense = expense,
            categoryName = uiState.categoriesMap[expense.category_id] ?: "Outros",
            onDismiss = { expenseToView = null },
            onChangeAdvanceDateClick = if (expense.supportsAdvance && expense.isAdvanced) {
                {
                    expenseToView = null
                    expenseToAdvance = expense
                }
            } else {
                null
            },
            onRemoveAdvanceClick = if (expense.supportsAdvance && expense.isAdvanced) {
                {
                    expenseToView = null
                    expenseToRemoveAdvance = expense
                }
            } else {
                null
            }
        )
    }

    expenseToAdvance?.let { expense ->
        AdvanceExpenseDialog(
            expense = expense,
            isUpdating = uiState.isUpdatingAdvanceStatus,
            onDismiss = { expenseToAdvance = null },
            onConfirm = { date ->
                val token = userToken ?: return@AdvanceExpenseDialog
                viewModel.updateAdvanceStatus(
                    token = token,
                    expense = expense,
                    isAdvanced = true,
                    advancedAt = date,
                    onUpdated = {
                        expenseToAdvance = null
                        refreshTrigger++
                    }
                )
            }
        )
    }

    expenseToRemoveAdvance?.let { expense ->
        RemoveAdvanceDialog(
            expense = expense,
            isUpdating = uiState.isUpdatingAdvanceStatus,
            onDismiss = { expenseToRemoveAdvance = null },
            onConfirm = {
                val token = userToken ?: return@RemoveAdvanceDialog
                viewModel.updateAdvanceStatus(
                    token = token,
                    expense = expense,
                    isAdvanced = false,
                    advancedAt = null,
                    onUpdated = {
                        expenseToRemoveAdvance = null
                        refreshTrigger++
                    }
                )
            }
        )
    }
}

@Composable
fun DespesaListItem(
    expense: Expense,
    categoriesMap: Map<Int, String>,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPaymentStatusClick: () -> Unit,
    onAdvanceClick: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale("pt", "BR")) }
    val formattedDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date = parser.parse(expense.date)
        if (date != null) dateFormat.format(date) else "00/00"
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        "00/00"
    }

    val typeLabel = when {
        expense.type.equals("Parcelada", ignoreCase = true) ->
            "Parc. ${expense.current_installment}/${expense.installments}"

        expense.type.equals("Fixa", ignoreCase = true) -> "Fixa"
        else -> "Única"
    }

    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val formattedAmount = formatter.format(expense.amount)

    val categoryName = categoriesMap[expense.category_id] ?: "Outros"
    val paymentSource = if (expense.paymentSources().distinct().size > 1) {
        "Pag. dividido"
    } else {
        expense.paymentCardSourceLabel()
    }

    ExpenseCard(
        style = ExpenseCardStyle.Detailed,
        title = expense.displayDescription,
        categoryName = categoryName,
        paymentSource = paymentSource,
        type = typeLabel,
        date = formattedDate,
        value = "- $formattedAmount",
        expenseType = expense.type,
        isPaid = expense.is_paid,
        isAdvanced = expense.isAdvanced,
        advancedLabel = formatAdvancedDate(expense.advancedAt)?.let { "Adiantada em $it" },
        showDate = !expense.isAdvanced,
        onView = onView,
        onEdit = onEdit,
        onDelete = onDelete,
        onPaymentStatusClick = onPaymentStatusClick,
        onAdvanceClick = onAdvanceClick
    )
}

@Composable
private fun AdvancedExpensesImpactNotice(
    selectedMonth: Int,
    selectedYear: Int
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PrimaryBlue.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimaryBlue.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Impactam o planejamento deste mês",
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Despesas previstas para outros meses foram incluídas em ${monthName(selectedMonth)} de $selectedYear.",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AdvancedExpenseImpactCard(
    expense: Expense,
    categoriesMap: Map<Int, String>,
    onView: () -> Unit,
    onGoToOriginalExpense: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val categoryName = categoriesMap[expense.category_id] ?: "Outros"
    val formattedAmount = remember(expense.amount) {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(expense.amount)
    }
    val scheduledDate = formatAdvancedDate(expense.date) ?: "data não informada"
    val advancedDate = formatAdvancedDate(expense.advancedAt) ?: "mês selecionado"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.displayDescription,
                        color = colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = categoryName,
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                FinancialValue(
                    text = "- $formattedAmount",
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Text(
                text = "Prevista para $scheduledDate · Considerada em $advancedDate",
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Visualizar", maxLines = 1)
                }
                Button(
                    onClick = onGoToOriginalExpense,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ir para despesa", maxLines = 1)
                }
            }
        }
        }
    }
