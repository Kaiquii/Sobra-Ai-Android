package com.example.appfinanceiro.feature.relatorios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.relatorios.components.CategoryExpensesCard
import com.example.appfinanceiro.feature.relatorios.components.IncomeVsExpenseCard
import com.example.appfinanceiro.feature.relatorios.components.MonthComparisonSection
import com.example.appfinanceiro.feature.relatorios.components.YearSummarySection
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    onNavigate: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onInstallmentsClick: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    viewModel: RelatoriosViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    var currentMonthIndex by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var compareMonthIndex by remember {
        val previousMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        mutableIntStateOf(previousMonth.get(Calendar.MONTH))
    }
    var compareYear by remember {
        val previousMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        mutableIntStateOf(previousMonth.get(Calendar.YEAR))
    }
    var selectedRange by remember { mutableStateOf(ReportRange.ONE_MONTH) }

    val currentMonthNumber = currentMonthIndex + 1
    val compareMonthNumber = compareMonthIndex + 1

    fun changeMonth(amount: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonthIndex)
            add(Calendar.MONTH, amount)
        }

        val previousMonth = cal.clone() as Calendar
        previousMonth.add(Calendar.MONTH, -1)

        currentMonthIndex = cal.get(Calendar.MONTH)
        currentYear = cal.get(Calendar.YEAR)
        compareMonthIndex = previousMonth.get(Calendar.MONTH)
        compareYear = previousMonth.get(Calendar.YEAR)
    }

    fun changeCompareMonth(amount: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, compareYear)
            set(Calendar.MONTH, compareMonthIndex)
            add(Calendar.MONTH, amount)
            if (
                get(Calendar.MONTH) == currentMonthIndex &&
                get(Calendar.YEAR) == currentYear
            ) {
                add(Calendar.MONTH, amount)
            }
        }
        compareMonthIndex = cal.get(Calendar.MONTH)
        compareYear = cal.get(Calendar.YEAR)
    }

    LaunchedEffect(currentMonthIndex, currentYear, userToken) {
        userToken?.let { token ->
            viewModel.loadReports(
                token = token,
                month = currentMonthNumber,
                year = currentYear
            )
        }
    }

    LaunchedEffect(currentMonthIndex, currentYear, compareMonthIndex, compareYear, userToken) {
        if (compareMonthIndex == currentMonthIndex && compareYear == currentYear) {
            val previousMonth = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonthIndex)
                add(Calendar.MONTH, -1)
            }
            compareMonthIndex = previousMonth.get(Calendar.MONTH)
            compareYear = previousMonth.get(Calendar.YEAR)
            return@LaunchedEffect
        }

        userToken?.let { token ->
            viewModel.loadMonthComparison(
                token = token,
                month = currentMonthNumber,
                year = currentYear,
                compareMonth = compareMonthNumber,
                compareYear = compareYear
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
        modifier = Modifier.swipeNavigation(2, onNavigate),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Relatórios",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(0) }) {
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
                itemSelecionado = 2,
                onItemClick = onNavigate,
                onAddClick = onAddClick
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "month_selector", contentType = "selector") {
                    MonthSelector(
                        monthIndex = currentMonthIndex,
                        currentYear = currentYear,
                        onPrevClick = { changeMonth(-1) },
                        onNextClick = { changeMonth(1) }
                    )
                }

                item(key = "installments_button", contentType = "action") {
                    Button(
                        onClick = onInstallmentsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compromissos Parcelados")
                    }
                }

                item(key = "category_expenses", contentType = "card") {
                    CategoryExpensesCard(
                        totalExpense = uiState.summaryData?.total_expense ?: 0.0,
                        categories = uiState.categoryData
                    )
                }

                item(key = "month_comparison", contentType = "card") {
                    MonthComparisonSection(
                        data = uiState.monthComparison,
                        isLoading = uiState.isComparisonLoading,
                        errorMessage = uiState.comparisonErrorMessage,
                        currentMonthIndex = currentMonthIndex,
                        currentYear = currentYear,
                        compareMonthIndex = compareMonthIndex,
                        compareYear = compareYear,
                        onPrevCompareClick = { changeCompareMonth(-1) },
                        onNextCompareClick = { changeCompareMonth(1) }
                    )
                }

                item(key = "income_vs_expense", contentType = "card") {
                    IncomeVsExpenseCard(
                        summaryData = uiState.summaryData,
                        chartData = uiState.chartData,
                        selectedRange = selectedRange,
                        currentMonth = currentMonthNumber,
                        onRangeSelected = { selectedRange = it }
                    )
                }

                item(key = "year_summary", contentType = "section") {
                    YearSummarySection(yearlySummary = uiState.yearlySummary)
                }

                item(key = "bottom_space", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
