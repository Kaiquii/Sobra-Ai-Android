package com.example.appfinanceiro.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.ExitConfirmationDialog
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.network.Expense
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import com.example.appfinanceiro.feature.home.components.DespesasSection
import com.example.appfinanceiro.feature.home.components.FilterOptionItem
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.home.components.ResumoFinanceiroSection
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Int) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)

    var currentMonthIndex by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    var summaryData by remember { mutableStateOf<SummaryResponse?>(null) }
    var expensesData by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var categoriesMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var incomesData by remember { mutableStateOf<List<Income>>(emptyList()) }

    var isSummaryLoading by remember { mutableStateOf(false) }
    var isIncomesLoading by remember { mutableStateOf(false) }
    var isExpensesLoading by remember { mutableStateOf(false) }

    var summaryError by remember { mutableStateOf<String?>(null) }
    var incomesError by remember { mutableStateOf<String?>(null) }
    var expensesError by remember { mutableStateOf<String?>(null) }

    var showCategoryFilterModal by remember { mutableStateOf(false) }
    var showPaymentSourceFilterModal by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedPaymentSource by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    var refreshIncomeActions by remember { mutableIntStateOf(0) }

    val filteredExpenses = expensesData.filter { expense ->
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

    val salarioAtual = incomesData.firstOrNull {
        (it.source.equals("Salario", ignoreCase = true) ||
                it.source.equals("Salário", ignoreCase = true)) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    }

    val adiantamentoAtual = incomesData.firstOrNull {
        it.source.equals("Adiantamento", ignoreCase = true) &&
                it.month == currentMonthIndex + 1 &&
                it.year == currentYear
    }

    val rendaExtraAtual = incomesData.firstOrNull {
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

    fun loadSummary() {
        val token = userToken ?: return

        coroutineScope.launch {
            isSummaryLoading = true
            summaryError = null

            try {
                summaryData = RetrofitClient.financeApi.getSummary(
                    token = "Bearer $token",
                    month = currentMonthIndex + 1,
                    year = currentYear
                )
            } catch (e: Exception) {
                android.util.Log.e("API_ERRO", "Falha ao carregar resumo", e)
                summaryError = "Não foi possível carregar o resumo financeiro."
            } finally {
                isSummaryLoading = false
            }
        }
    }

    fun loadIncomes() {
        val token = userToken ?: return

        coroutineScope.launch {
            isIncomesLoading = true
            incomesError = null

            try {
                val response = RetrofitClient.financeApi.getIncomes(
                    token = "Bearer $token",
                    month = currentMonthIndex + 1,
                    year = currentYear
                )

                incomesData = response.incomes

                val salarioFromIncomes = response.incomes
                    .filter {
                        it.source.equals("Salario", ignoreCase = true) ||
                                it.source.equals("Salário", ignoreCase = true)
                    }
                    .sumOf { it.amount }

                val adiantamentoFromIncomes = response.incomes
                    .filter { it.source.equals("Adiantamento", ignoreCase = true) }
                    .sumOf { it.amount }

                summaryData = summaryData?.copy(
                    salario = salarioFromIncomes,
                    adiantamento = adiantamentoFromIncomes
                )
            } catch (e: Exception) {
                android.util.Log.e("API_ERRO", "Falha ao carregar rendas", e)
                incomesError = "Não foi possível carregar as rendas."
            } finally {
                isIncomesLoading = false
            }
        }
    }

    fun loadExpenses() {
        val token = userToken ?: return

        coroutineScope.launch {
            isExpensesLoading = true
            expensesError = null

            try {
                val catResponse = RetrofitClient.financeApi.getCategories("Bearer $token")
                categoriesMap = catResponse.categories.associate { it.id to it.name }

                val expResponse = RetrofitClient.financeApi.getExpenses(
                    token = "Bearer $token",
                    month = currentMonthIndex + 1,
                    year = currentYear
                )

                expensesData = expResponse.expenses
            } catch (e: Exception) {
                android.util.Log.e("API_ERRO", "Falha ao carregar despesas", e)
                expensesError = "Não foi possível carregar as despesas."
            } finally {
                isExpensesLoading = false
            }
        }
    }

    LaunchedEffect(currentMonthIndex, currentYear, userToken, refreshIncomeActions) {
        if (userToken != null) {
            loadSummary()
            loadIncomes()
            loadExpenses()
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
                    isLoading = isSummaryLoading || isIncomesLoading,
                    errorMessage = summaryError ?: incomesError,
                    onRetry = {
                        loadSummary()
                        loadIncomes()
                    },
                    data = summaryData,
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
                DespesasSection(
                    isLoading = isExpensesLoading,
                    errorMessage = expensesError,
                    onRetry = { loadExpenses() },
                    expenses = filteredExpenses,
                    categoriesMap = categoriesMap,
                    onCategoryFilterClick = { showCategoryFilterModal = true },
                    onPaymentSourceFilterClick = { showPaymentSourceFilterModal = true },
                    isCategoryFiltered = selectedCategoryId != null,
                    isPaymentSourceFiltered = selectedPaymentSource != null,
                    onAddClick = onAddClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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

                categoriesMap.forEach { (id, name) ->
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
