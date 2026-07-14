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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.ExpenseDetailsDialog
import com.example.appfinanceiro.core.designsystem.components.ExpenseCard
import com.example.appfinanceiro.core.designsystem.components.ExpenseCardStyle
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.home.utils.getCategoryIconAndColor
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
    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = colorScheme.background
    val inputBgColor = colorScheme.surface
    val textColor = colorScheme.onBackground
    val surfaceTextColor = colorScheme.onSurface
    val secondaryTextColor = colorScheme.onSurfaceVariant

    var refreshTrigger by remember { mutableIntStateOf(0) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }

    val calendar = remember { Calendar.getInstance() }
    var currentMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToView by remember { mutableStateOf<Expense?>(null) }
    val searchCurrencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    LaunchedEffect(currentMonthIndex, currentYear, userToken, refreshTrigger) {
        userToken?.let { token ->
            viewModel.loadExpenses(token, currentMonthIndex + 1, currentYear)
        }
    }

    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            sessionManager.clearSession()
            viewModel.clearSessionExpired()
            onSessionExpired()
        }
    }

    LaunchedEffect(uiState.deleteSuccessMessage, uiState.deleteErrorMessage) {
        uiState.deleteSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        uiState.deleteErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    fun matchesExpenseType(expense: Expense, filter: String): Boolean {
        return when (filter) {
            "Parceladas" -> expense.type.equals("Parcelada", ignoreCase = true)
            "Únicas" -> expense.type.equals("Única", ignoreCase = true) ||
                    expense.type.equals("Unica", ignoreCase = true)
            "Fixas" -> expense.type.equals("Fixa", ignoreCase = true)
            else -> true
        }
    }

    val searchMatchedExpenses = uiState.expensesData.filter { expense ->
        val trimmedQuery = searchQuery.trim()
        val normalizedQuery = normalizeAmountSearchText(trimmedQuery)
        val normalizedCurrencyAmount = normalizeAmountSearchText(
            searchCurrencyFormatter.format(expense.amount)
        )
        val normalizedRawAmount = normalizeAmountSearchText(expense.amount.toString())

        trimmedQuery.isBlank() ||
                expense.description.contains(trimmedQuery, ignoreCase = true) ||
                (
                        normalizedQuery.isNotBlank() &&
                                (
                                        normalizedCurrencyAmount.contains(normalizedQuery) ||
                                                normalizedRawAmount.contains(normalizedQuery)
                                        )
                        )
    }
    val expenseFilters = listOf("Todas", "Parceladas", "Únicas", "Fixas")
    val expenseCountsByFilter = expenseFilters.associateWith { filter ->
        searchMatchedExpenses.count { expense -> matchesExpenseType(expense, filter) }
    }
    val filteredExpenses = searchMatchedExpenses.filter { expense ->
        matchesExpenseType(expense, selectedFilter)
    }
    val selectedFilterCount = expenseCountsByFilter[selectedFilter] ?: filteredExpenses.size

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
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                onPrevClick = {
                    if (currentMonthIndex == 0) {
                        currentMonthIndex = 11
                        currentYear--
                    } else {
                        currentMonthIndex--
                    }
                },
                onNextClick = {
                    if (currentMonthIndex == 11) {
                        currentMonthIndex = 0
                        currentYear++
                    } else {
                        currentMonthIndex++
                    }
                },
                centerSuffix = if (selectedFilterCount == 1) {
                    "1 despesa"
                } else {
                    "$selectedFilterCount despesas"
                }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma despesa encontrada.", color = secondaryTextColor)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        DespesaListItem(
                            expense = expense,
                            categoriesMap = uiState.categoriesMap,
                            onView = { expenseToView = expense },
                            onEdit = { onEditClick(expense.id) },
                            onDelete = { expenseToDelete = expense }
                        )
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

    expenseToView?.let { expense ->
        ExpenseDetailsDialog(
            expense = expense,
            categoryName = uiState.categoriesMap[expense.category_id] ?: "Outros",
            onDismiss = { expenseToView = null }
        )
    }
}

private fun normalizeAmountSearchText(value: String): String {
    return value
        .lowercase(Locale("pt", "BR"))
        .replace("r$", "")
        .filter { it.isDigit() || it == ',' || it == '.' }
        .replace(',', '.')
}

@Composable
fun DespesaListItem(
    expense: Expense,
    categoriesMap: Map<Int, String>,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
    val paymentSource = expense.payment_source ?: "Salário"
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
        onView = onView,
        onEdit = onEdit,
        onDelete = onDelete
    )
}
