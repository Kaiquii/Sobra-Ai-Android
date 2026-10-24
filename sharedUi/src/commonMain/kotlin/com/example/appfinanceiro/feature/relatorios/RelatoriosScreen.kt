package com.example.appfinanceiro.feature.relatorios

import br.com.sobraai.ui.runtime.UiToast as Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.date.shiftMonth
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyButton
import com.example.appfinanceiro.core.designsystem.components.PrivateReportContent
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.feature.home.components.MonthSelector
import com.example.appfinanceiro.feature.relatorios.components.CategoryExpensesCard
import com.example.appfinanceiro.feature.relatorios.components.IncomeVsExpenseCard
import com.example.appfinanceiro.feature.relatorios.components.MonthComparisonSection
import com.example.appfinanceiro.feature.relatorios.components.YearSummarySection
import com.example.appfinanceiro.feature.relatorios.export.ExportedReport
import com.example.appfinanceiro.feature.relatorios.export.ReportExportRequest
import com.example.appfinanceiro.feature.relatorios.export.ReportExportSheet
import com.example.appfinanceiro.feature.relatorios.export.ReportExportUiState
import com.example.appfinanceiro.feature.relatorios.export.ReportExportViewModel
import com.example.appfinanceiro.feature.relatorios.export.fallbackFileName
import br.com.sobraai.ui.runtime.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    onNavigate: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onInstallmentsClick: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    viewModel: RelatoriosViewModel = rememberPresentation("RelatoriosViewModel") { services -> RelatoriosViewModel(services.finance) },
    exportViewModel: ReportExportViewModel = rememberPresentation("ReportExportViewModel") { services -> ReportExportViewModel(services) }
) {
    val context = LocalUiServices.current
    val sessionManager = remember { context.session }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val financeRevision by FinanceDataChanges.revision.collectAsState()
    val exportState by exportViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    val initialDate = remember { Calendar.getInstance() }
    val initialMonthIndex = initialDate.get(Calendar.MONTH)
    val initialYear = initialDate.get(Calendar.YEAR)
    val initialPreviousMonth = remember(initialMonthIndex, initialYear) {
        shiftMonth(initialMonthIndex, initialYear, -1)
    }
    var currentMonthIndex by rememberSaveable { mutableIntStateOf(initialMonthIndex) }
    var currentYear by rememberSaveable { mutableIntStateOf(initialYear) }
    var compareMonthIndex by rememberSaveable {
        mutableIntStateOf(initialPreviousMonth.monthIndex)
    }
    var compareYear by rememberSaveable {
        mutableIntStateOf(initialPreviousMonth.year)
    }
    var selectedRange by rememberSaveable { mutableStateOf(ReportRange.ONE_MONTH) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var pendingLegacyRequest by remember { mutableStateOf<ReportExportRequest?>(null) }

    val currentMonthNumber = currentMonthIndex + 1
    val compareMonthNumber = compareMonthIndex + 1
    val hasCachedReportData = uiState.summaryData != null ||
            uiState.categoryData.isNotEmpty() ||
            uiState.chartData.isNotEmpty() ||
            uiState.yearlySummary != null
    val reportErrorBannerMessage = uiState.errorMessage?.let { error ->
        dataRequestErrorMessage(
            errorMessage = error,
            showingPreviousData = hasCachedReportData,
            dataLabel = "os relatórios"
        )
    }


    fun changeMonth(amount: Int) {
        val target = shiftMonth(currentMonthIndex, currentYear, amount)
        val previousMonth = shiftMonth(target.monthIndex, target.year, -1)

        currentMonthIndex = target.monthIndex
        currentYear = target.year
        compareMonthIndex = previousMonth.monthIndex
        compareYear = previousMonth.year
    }

    fun changeCompareMonth(amount: Int) {
        var target = shiftMonth(compareMonthIndex, compareYear, amount)
        if (
            target.monthIndex == currentMonthIndex &&
            target.year == currentYear
        ) {
            target = shiftMonth(target.monthIndex, target.year, amount)
        }
        compareMonthIndex = target.monthIndex
        compareYear = target.year
    }

    LaunchedEffect(currentMonthIndex, currentYear, userToken, financeRevision) {
        userToken?.let { token ->
            viewModel.loadReports(
                token = token,
                month = currentMonthNumber,
                year = currentYear,
                refreshKey = financeRevision
            )
        }
    }

    LaunchedEffect(currentMonthIndex, currentYear, compareMonthIndex, compareYear, userToken, financeRevision) {
        if (compareMonthIndex == currentMonthIndex && compareYear == currentYear) {
            val previousMonth = shiftMonth(currentMonthIndex, currentYear, -1)
            compareMonthIndex = previousMonth.monthIndex
            compareYear = previousMonth.year
            return@LaunchedEffect
        }

        userToken?.let { token ->
            viewModel.loadMonthComparison(
                token = token,
                month = currentMonthNumber,
                year = currentYear,
                compareMonth = compareMonthNumber,
                compareYear = compareYear,
                refreshKey = financeRevision
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

    LaunchedEffect(exportState) {
        when (exportState) {
            ReportExportUiState.SessionExpired -> {
                sessionManager.clearSession()
                exportViewModel.clearResult()
                showExportSheet = false
                onSessionExpired()
            }
            is ReportExportUiState.Success -> showExportSheet = false
            else -> Unit
        }
    }



    fun startExport(request: ReportExportRequest) {
        userToken?.let { exportViewModel.export(it, request) }
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
                itemSelecionado = 2,
                onItemClick = onNavigate,
                onAddClick = onAddClick
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && !uiState.hasLoadedOnce) {
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
                state = listState,
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

                reportErrorBannerMessage?.let { message ->
                    item(key = "report_error", contentType = "status") {
                        AppDataErrorBanner(
                            message = message,
                            isRetrying = uiState.isLoading,
                            onRetry = {
                                userToken?.let { token ->
                                    viewModel.loadReports(token, currentMonthNumber, currentYear)
                                }
                            }
                        )
                    }
                }

                item(key = "installments_button", contentType = "action") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportViewModel.clearResult()
                                    showExportSheet = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Exportar relatórios",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }

                            Button(
                                onClick = onInstallmentsClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Compromissos parcelados",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    item(key = "category_expenses", contentType = "card") {
                        PrivateReportContent("Gastos por Categoria") {
                            CategoryExpensesCard(
                                totalExpense = uiState.summaryData?.total_expense ?: 0.0,
                                categories = uiState.categoryData
                            )
                        }
                    }

                item(key = "month_comparison", contentType = "card") {
                    PrivateReportContent("Comparativo Mensal") {
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
                }

                item(key = "income_vs_expense", contentType = "card") {
                    PrivateReportContent("Receitas e Despesas") {
                        IncomeVsExpenseCard(
                            summaryData = uiState.summaryData,
                            chartData = uiState.chartData,
                            selectedRange = selectedRange,
                            currentMonth = currentMonthNumber,
                            onRangeSelected = { selectedRange = it }
                        )
                    }
                }

                item(key = "year_summary", contentType = "section") {
                    PrivateReportContent("Resumo Anual") {
                        YearSummarySection(yearlySummary = uiState.yearlySummary)
                    }
                }

                item(key = "bottom_space", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showExportSheet) {
        ReportExportSheet(
            month = currentMonthNumber,
            year = currentYear,
            initialCompareMonth = compareMonthNumber,
            initialCompareYear = compareYear,
            isExporting = exportState is ReportExportUiState.Exporting,
            errorMessage = (exportState as? ReportExportUiState.Error)?.message,
            onDismiss = {
                showExportSheet = false
                exportViewModel.clearResult()
            },
            onExport = ::startExport,
            onCancelExport = exportViewModel::cancelExport
        )
    }

    (exportState as? ReportExportUiState.Success)?.let { success ->
        ExportSuccessDialog(
            report = success.report,
            onOpen = {
                try { context.reports.open(success.report) }
                catch (_: Exception) { context.notify("Nenhum aplicativo disponível consegue abrir este formato.") }
            },
            onShare = {
                try { context.reports.share(success.report) }
                catch (_: Exception) { context.notify("Não foi possível compartilhar o arquivo.") }
            },
            onDismiss = exportViewModel::clearResult
        )
    }
}

@Composable
private fun ExportSuccessDialog(
    report: ExportedReport,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.background,
        titleContentColor = colorScheme.onBackground,
        textContentColor = colorScheme.onBackground,
        title = { Text("Relatório salvo", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "${report.fileName}\n\nO arquivo está pronto para abrir ou compartilhar."
            )
        },
        confirmButton = {
            Button(onClick = onOpen) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartilhar")
                }
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        }
    )
}

