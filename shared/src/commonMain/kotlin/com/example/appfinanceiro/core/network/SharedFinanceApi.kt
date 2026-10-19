package com.example.appfinanceiro.core.network

interface SharedFinanceApi {
    suspend fun getAppVersion(
        platform: String = "android"
    ): AppVersionResponse

    suspend fun chatAssistant(
        token: String,
        request: AssistantChatRequest
    ): AssistantChatResponse

    suspend fun getAssistantConversations(
        token: String
    ): AssistantConversationsResponse

    suspend fun getAssistantMessages(
        token: String,
        id: Int
    ): AssistantMessagesResponse

    suspend fun deleteAssistantConversation(
        token: String,
        id: Int
    ): DefaultResponse

    suspend fun getSummary(
        token: String,
        month: Int,
        year: Int
    ): SummaryResponse

    suspend fun getCategories(
        token: String
    ): CategoriesResponse

    suspend fun getExpenses(
        token: String,
        month: Int,
        year: Int,
        paymentStatus: String? = null,
        periodMode: String? = null
    ): ExpensesResponse

    suspend fun getIncomes(
        token: String,
        month: Int? = null,
        year: Int? = null
    ): IncomesResponse

    suspend fun createExpense(
        token: String,
        request: ExpenseRequest
    ): DefaultResponse

    suspend fun updateExpense(
        token: String,
        id: Int,
        request: ExpenseUpdateRequest
    ): DefaultResponse

    suspend fun updateExpensePaymentStatus(
        token: String,
        id: Int,
        request: ExpensePaymentStatusRequest
    ): ExpensePaymentStatusResponse

    suspend fun updateAdvanceStatus(
        token: String,
        id: Int,
        request: UpdateAdvanceStatusRequest
    ): AdvanceStatusResponse

    suspend fun deleteExpense(
        token: String,
        id: Int,
        deleteFuture: Boolean? = null
    ): DefaultResponse

    suspend fun getExpenseById(
        token: String,
        id: Int
    ): Expense

    suspend fun createIncome(
        token: String,
        request: IncomeRequest
    ): DefaultResponse

    suspend fun updateIncome(
        token: String,
        id: Int,
        request: IncomeUpdateRequest
    ): DefaultResponse

    suspend fun deleteIncome(
        token: String,
        id: Int,
        deleteFuture: Boolean? = null
    ): DefaultResponse

    suspend fun createCategory(
        token: String,
        request: CategoryRequest
    ): CategoryResponse

    suspend fun updateCategory(
        token: String,
        id: Int,
        request: CategoryRequest
    ): DefaultResponse

    suspend fun deleteCategory(
        token: String,
        id: Int
    ): DefaultResponse

    suspend fun updateProfile(
        token: String,
        request: UpdateProfileRequest
    ): DefaultResponse

    suspend fun getProfile(
        token: String
    ): ProfileResponse

    suspend fun deleteProfilePhoto(
        token: String
    ): DefaultResponse

    suspend fun getReportCategories(
        token: String,
        month: Int,
        year: Int
    ): List<CategoryReportResponse>?

    suspend fun getReportChart(
        token: String,
        year: Int
    ): List<ChartReportResponse>

    suspend fun getYearlySummary(
        token: String,
        year: Int
    ): YearlySummaryResponse

    suspend fun getMonthComparison(
        token: String,
        month: Int,
        year: Int,
        compareMonth: Int? = null,
        compareYear: Int? = null
    ): MonthComparisonResponse

    suspend fun getInstallmentCommitments(
        token: String,
        months: Int = 12,
        month: Int? = null,
        year: Int? = null,
        includeCurrentMonthAsPaid: Boolean = false
    ): InstallmentCommitmentsResponse

}
