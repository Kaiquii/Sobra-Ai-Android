package com.example.appfinanceiro.core.network

import kotlinx.serialization.encodeToString

class KtorFinanceApi(private val transport: ApiTransport) : SharedFinanceApi {
    override suspend fun getAppVersion(
        platform: String
    ): AppVersionResponse =
        transport.request(
            method = "GET", path = "api/app-version",
            authenticated = false,
            query = mapOf("platform" to platform),
        )

    override suspend fun chatAssistant(
        token: String,
        request: AssistantChatRequest
    ): AssistantChatResponse =
        transport.request(
            method = "POST", path = "api/assistant/chat",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun getAssistantConversations(
        token: String
    ): AssistantConversationsResponse =
        transport.request(
            method = "GET", path = "api/assistant/conversations",
            authorization = token,
        )

    override suspend fun getAssistantMessages(
        token: String,
        id: Int
    ): AssistantMessagesResponse =
        transport.request(
            method = "GET", path = "api/assistant/conversations/${id}/messages",
            authorization = token,
        )

    override suspend fun deleteAssistantConversation(
        token: String,
        id: Int
    ): DefaultResponse =
        transport.request(
            method = "DELETE", path = "api/assistant/conversations/${id}",
            authorization = token,
        )

    override suspend fun getSummary(
        token: String,
        month: Int,
        year: Int
    ): SummaryResponse =
        transport.request(
            method = "GET", path = "api/reports/summary",
            authorization = token,
            query = mapOf("month" to month, "year" to year),
        )

    override suspend fun getCategories(
        token: String
    ): CategoriesResponse =
        transport.request(
            method = "GET", path = "api/categories/",
            authorization = token,
        )

    override suspend fun getExpenses(
        token: String,
        month: Int,
        year: Int,
        paymentStatus: String?,
        periodMode: String?
    ): ExpensesResponse =
        transport.request(
            method = "GET", path = "api/expenses",
            authorization = token,
            query = mapOf("month" to month, "year" to year, "payment_status" to paymentStatus, "period_mode" to periodMode),
        )

    override suspend fun getIncomes(
        token: String,
        month: Int?,
        year: Int?
    ): IncomesResponse =
        transport.request(
            method = "GET", path = "api/incomes/",
            authorization = token,
            query = mapOf("month" to month, "year" to year),
        )

    override suspend fun createExpense(
        token: String,
        request: ExpenseRequest
    ): DefaultResponse =
        transport.request(
            method = "POST", path = "api/expenses/",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun updateExpense(
        token: String,
        id: Int,
        request: ExpenseUpdateRequest
    ): DefaultResponse =
        transport.request(
            method = "PATCH", path = "api/expenses/${id}",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun updateExpensePaymentStatus(
        token: String,
        id: Int,
        request: ExpensePaymentStatusRequest
    ): ExpensePaymentStatusResponse =
        transport.request(
            method = "PATCH", path = "api/expenses/${id}/payment-status",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun updateAdvanceStatus(
        token: String,
        id: Int,
        request: UpdateAdvanceStatusRequest
    ): AdvanceStatusResponse =
        transport.request(
            method = "PATCH", path = "api/expenses/${id}/advance-status",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun deleteExpense(
        token: String,
        id: Int,
        deleteFuture: Boolean?
    ): DefaultResponse =
        transport.request(
            method = "DELETE", path = "api/expenses/${id}",
            authorization = token,
            query = mapOf("delete_future" to deleteFuture),
        )

    override suspend fun getExpenseById(
        token: String,
        id: Int
    ): Expense =
        transport.request(
            method = "GET", path = "api/expenses/${id}",
            authorization = token,
        )

    override suspend fun createIncome(
        token: String,
        request: IncomeRequest
    ): DefaultResponse =
        transport.request(
            method = "POST", path = "api/incomes/",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun updateIncome(
        token: String,
        id: Int,
        request: IncomeUpdateRequest
    ): DefaultResponse =
        transport.request(
            method = "PATCH", path = "api/incomes/${id}",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun deleteIncome(
        token: String,
        id: Int,
        deleteFuture: Boolean?
    ): DefaultResponse =
        transport.request(
            method = "DELETE", path = "api/incomes/${id}",
            authorization = token,
            query = mapOf("delete_future" to deleteFuture),
        )

    override suspend fun createCategory(
        token: String,
        request: CategoryRequest
    ): CategoryResponse =
        transport.request(
            method = "POST", path = "api/categories/",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun updateCategory(
        token: String,
        id: Int,
        request: CategoryRequest
    ): DefaultResponse =
        transport.request(
            method = "PATCH", path = "api/categories/${id}",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun deleteCategory(
        token: String,
        id: Int
    ): DefaultResponse =
        transport.request(
            method = "DELETE", path = "api/categories/${id}",
            authorization = token,
        )

    override suspend fun updateProfile(
        token: String,
        request: UpdateProfileRequest
    ): DefaultResponse =
        transport.request(
            method = "PATCH", path = "api/users/profile",
            authorization = token,
            body = FinanceJson.encodeToString(request),
        )

    override suspend fun getProfile(
        token: String
    ): ProfileResponse =
        transport.request(
            method = "GET", path = "api/users/profile",
            authorization = token,
        )

    override suspend fun deleteProfilePhoto(
        token: String
    ): DefaultResponse =
        transport.request(
            method = "DELETE", path = "api/users/profile/photo",
            authorization = token,
            emptyResponse = "{\"message\":\"\"}",
        )

    override suspend fun getReportCategories(
        token: String,
        month: Int,
        year: Int
    ): List<CategoryReportResponse>? =
        transport.request(
            method = "GET", path = "api/reports/categories",
            authorization = token,
            query = mapOf("month" to month, "year" to year),
        )

    override suspend fun getReportChart(
        token: String,
        year: Int
    ): List<ChartReportResponse> =
        transport.request(
            method = "GET", path = "api/reports/chart",
            authorization = token,
            query = mapOf("year" to year),
        )

    override suspend fun getYearlySummary(
        token: String,
        year: Int
    ): YearlySummaryResponse =
        transport.request(
            method = "GET", path = "api/reports/yearly-summary",
            authorization = token,
            query = mapOf("year" to year),
        )

    override suspend fun getMonthComparison(
        token: String,
        month: Int,
        year: Int,
        compareMonth: Int?,
        compareYear: Int?
    ): MonthComparisonResponse =
        transport.request(
            method = "GET", path = "api/reports/month-comparison",
            authorization = token,
            query = mapOf("month" to month, "year" to year, "compare_month" to compareMonth, "compare_year" to compareYear),
        )

    override suspend fun getInstallmentCommitments(
        token: String,
        months: Int,
        month: Int?,
        year: Int?,
        includeCurrentMonthAsPaid: Boolean
    ): InstallmentCommitmentsResponse =
        transport.request(
            method = "GET", path = "api/reports/installment-commitments",
            authorization = token,
            query = mapOf("months" to months, "month" to month, "year" to year, "include_current_month_as_paid" to includeCurrentMonthAsPaid),
        )

}
