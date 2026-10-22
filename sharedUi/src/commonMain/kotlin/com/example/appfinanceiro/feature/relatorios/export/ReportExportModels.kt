package com.example.appfinanceiro.feature.relatorios.export


enum class ReportExportType(val apiValue: String, val label: String) {
    EXPENSES("expenses", "Despesas"),
    INCOMES("incomes", "Receitas"),
    CATEGORIES("categories", "Resumo por categoria"),
    SUMMARY("summary", "Resumo financeiro mensal"),
    MONTH_COMPARISON("month_comparison", "Comparativo mensal"),
    INSTALLMENT_COMMITMENTS("installment_commitments", "Compromissos parcelados"),
    FULL_REPORT("full_report", "Relatório completo");

    val supportsComparison: Boolean
        get() = this == MONTH_COMPARISON || this == FULL_REPORT

    val supportsInstallments: Boolean
        get() = this == INSTALLMENT_COMMITMENTS || this == FULL_REPORT
}

enum class ReportExportFormat(
    val apiValue: String,
    val label: String,
    val extension: String,
    val mimeType: String
) {
    PDF("pdf", "PDF", "pdf", "application/pdf"),
    XLSX(
        "xlsx",
        "Excel",
        "xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    CSV("csv", "CSV", "csv", "text/csv")
}

data class ReportExportRequest(
    val type: ReportExportType,
    val month: Int,
    val year: Int,
    val format: ReportExportFormat,
    val compareMonth: Int? = null,
    val compareYear: Int? = null,
    val months: Int? = null,
    val includeCurrentMonthAsPaid: Boolean? = null
)

data class ExportedReport(
    val uri: String,
    val fileName: String,
    val mimeType: String
)

sealed interface ReportExportUiState {
    data object Idle : ReportExportUiState
    data object Exporting : ReportExportUiState
    data class Success(val report: ExportedReport) : ReportExportUiState
    data class Error(val message: String) : ReportExportUiState
    data object SessionExpired : ReportExportUiState
}

fun fallbackFileName(request: ReportExportRequest): String =
    "${request.type.apiValue}-${request.year}-${request.month.toString().padStart(2, '0')}.${request.format.extension}"
