package com.example.appfinanceiro.feature.despesas

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.date.shiftMonth
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.ExpenseDetailsDialog
import com.example.appfinanceiro.core.designsystem.components.ExpenseCard
import com.example.appfinanceiro.core.designsystem.components.ExpenseCardStyle
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.paymentCardSourceLabel
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.home.utils.getCategoryIconAndColor
import com.example.appfinanceiro.feature.despesas.components.ExpenseFilterButton
import com.example.appfinanceiro.feature.despesas.components.ExpenseFiltersDialog
import com.example.appfinanceiro.feature.despesas.components.AdvanceExpenseDialog
import com.example.appfinanceiro.feature.despesas.components.RemoveAdvanceDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespesasScreen(
    onNavigate: (Int) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onSessionExpired: () -> Unit = {},
    viewModel: DespesasViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = colorScheme.background
    val inputBgColor = colorScheme.surface
    val textColor = colorScheme.onBackground
    val surfaceTextColor = colorScheme.onSurface
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

    LaunchedEffect(currentMonthIndex, currentYear, userToken, refreshTrigger) {
        userToken?.let { token ->
            viewModel.loadExpenses(
                token,
                currentMonthIndex + 1,
                currentYear
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
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.deleteErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.paymentStatusSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.paymentStatusErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.advanceStatusSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.advanceStatusErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
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
        else -> "Total"
    }
    val formattedSelectedFilterTotal = remember(selectedFilterTotal) {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
            .format(selectedFilterTotal)
    }
    val expensesErrorBannerMessage = uiState.errorMessage?.let { error ->
        dataRequestErrorMessage(
            errorMessage = error,
            showingPreviousData = uiState.expensesData.isNotEmpty(),
            dataLabel = "as despesas"
        )
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
                        AppLoadingIndicator(modifier = Modifier.padding(end = 16.dp))
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f),
                    placeholder = {
                        Text("Buscar despesa...", color = secondaryTextColor)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            tint = secondaryTextColor,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor,
                        disabledContainerColor = inputBgColor,
                        focusedTextColor = surfaceTextColor,
                        unfocusedTextColor = surfaceTextColor,
                        cursorColor = PrimaryBlue,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedPlaceholderColor = secondaryTextColor,
                        unfocusedPlaceholderColor = secondaryTextColor,
                        focusedLeadingIconColor = secondaryTextColor,
                        unfocusedLeadingIconColor = secondaryTextColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                ExpenseFilterButton(
                    active = selectedCategoryId != null || selectedPaymentSource != null ||
                        selectedPaymentStatus != null,
                    onClick = {
                        focusManager.clearFocus()
                        showFiltersDialog = true
                    }
                )
            }

            val filterChipBg = TextMuted.copy(alpha = 0.2f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expenseFilters.forEach { filter ->
                    val isSelected = selectedFilter == filter

                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) PrimaryBlue else filterChipBg,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            MonthSelector(
                monthIndex = currentMonthIndex,
                currentYear = currentYear,
                onPrevClick = { changeMonth(-1) },
                onNextClick = { changeMonth(1) },
                centerSuffix = when {
                    uiState.errorMessage != null && uiState.expensesData.isEmpty() -> null
                    selectedFilterCount == 1 -> "1 despesa"
                    else -> "$selectedFilterCount despesas"
                }
            )

            expensesErrorBannerMessage?.let { message ->
                AppDataErrorBanner(
                    message = message,
                    isRetrying = uiState.isLoading,
                    onRetry = {
                        userToken?.let { token ->
                            viewModel.loadExpenses(
                                token,
                                currentMonthIndex + 1,
                                currentYear
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.isLoading && !uiState.hasLoadedOnce) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AppLoadingIndicator(size = 40.dp, strokeWidth = 4.dp)
                }
            } else if (uiState.errorMessage != null && uiState.expensesData.isEmpty()) {
                Spacer(modifier = Modifier.fillMaxSize())
            } else if (filteredExpenses.isEmpty() && filteredIncomingAdvanced.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma despesa encontrada.", color = secondaryTextColor)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "expenses_summary") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFilterTotalLabel,
                                    color = secondaryTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formattedSelectedFilterTotal,
                                    color = PrimaryBlue,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            if (filteredExpenses.isNotEmpty()) {
                                Text(
                                    text = "Despesas previstas para ${monthName(currentMonthIndex + 1)}",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    items(
                        items = filteredExpenses,
                        key = { expense -> "scheduled_${expense.id}" }
                    ) { expense ->
                        DespesaListItem(
                            expense = expense,
                            categoriesMap = uiState.categoriesMap,
                            onView = { expenseToView = expense },
                            onEdit = { onEditClick(expense.id) },
                            onDelete = { expenseToDelete = expense },
                            onPaymentStatusClick = { expensePaymentStatusToChange = expense }
                        )
                    }

                    if (filteredIncomingAdvanced.isNotEmpty()) {
                        item(key = "incoming_advanced_header") {
                            AdvancedExpensesImpactNotice(
                                selectedMonth = currentMonthIndex + 1,
                                selectedYear = currentYear
                            )
                        }
                        items(
                            items = filteredIncomingAdvanced,
                            key = { expense -> "incoming_${expense.id}" }
                        ) { expense ->
                            AdvancedExpenseImpactCard(
                                expense = expense,
                                categoriesMap = uiState.categoriesMap,
                                onView = { expenseToView = expense },
                                onGoToOriginalExpense = {
                                    val scheduledPeriod = expense.scheduledMonthYear()
                                    if (scheduledPeriod == null) {
                                        Toast.makeText(
                                            context,
                                            "Não foi possível identificar o mês original desta despesa.",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                        "Tem certeza que deseja excluir '${expenseToDelete?.description}'?",
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
                        "Confirma que \"${expense.description}\" foi paga?"
                    } else {
                        "Confirma que deseja desmarcar \"${expense.description}\" como paga?"
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
            onAdvanceClick = {
                expenseToView = null
                expenseToAdvance = expense
            },
            onChangeAdvanceDateClick = if (expense.isAdvanced) {
                {
                    expenseToView = null
                    expenseToAdvance = expense
                }
            } else {
                null
            },
            onRemoveAdvanceClick = if (expense.isAdvanced) {
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
    onPaymentStatusClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale("pt", "BR")) }
    val formattedDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date = parser.parse(expense.date)
        if (date != null) dateFormat.format(date) else "00/00"
    } catch (e: Exception) {
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
    val paymentSource = if (expense.payment_splits.size > 1) {
        "Pag. dividido"
    } else {
        expense.paymentCardSourceLabel()
    }
    val (icon, color) = getCategoryIconAndColor(categoryName)

    ExpenseCard(
        style = ExpenseCardStyle.Detailed,
        icon = icon,
        iconColor = color,
        title = expense.description,
        categoryName = categoryName,
        paymentSource = paymentSource,
        type = typeLabel,
        date = formattedDate,
        value = "- $formattedAmount",
        notes = expense.notes,
        isPaid = expense.is_paid,
        isAdvanced = expense.isAdvanced,
        advancedLabel = formatAdvancedDate(expense.advancedAt)?.let { "Adiantada em $it" },
        showDate = !expense.isAdvanced,
        onView = onView,
        onEdit = onEdit,
        onDelete = onDelete,
        onPaymentStatusClick = onPaymentStatusClick
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
                        text = expense.description,
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
                Text(
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
