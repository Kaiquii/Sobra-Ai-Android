package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.AssistantChatResponse
import com.example.appfinanceiro.core.network.AssistantConversationsResponse
import com.example.appfinanceiro.core.network.AssistantMessagesResponse
import com.example.appfinanceiro.core.network.CategoriesResponse
import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.core.network.DefaultResponse
import com.example.appfinanceiro.core.network.ExpensesResponse
import com.example.appfinanceiro.core.network.IncomesResponse
import com.example.appfinanceiro.core.network.InstallmentCommitmentsResponse
import com.example.appfinanceiro.core.network.MonthComparisonResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.network.YearlySummaryResponse

interface HomeDataSource {
    suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse
    suspend fun getIncomes(token: String, month: Int? = null, year: Int? = null): IncomesResponse
    suspend fun getCategories(token: String): CategoriesResponse
    suspend fun getExpenses(token: String, month: Int, year: Int): ExpensesResponse
}

interface ExpensesDataSource {
    suspend fun getCategories(token: String): CategoriesResponse
    suspend fun getExpenses(token: String, month: Int, year: Int): ExpensesResponse
    suspend fun deleteExpense(token: String, id: Int, deleteFuture: Boolean? = null): DefaultResponse
}

interface ReportsDataSource {
    suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse
    suspend fun getReportCategories(token: String, month: Int, year: Int): List<CategoryReportResponse>
    suspend fun getReportChart(token: String, year: Int): List<ChartReportResponse>
    suspend fun getYearlySummary(token: String, year: Int): YearlySummaryResponse
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

interface AssistantDataSource {
    suspend fun chatAssistant(token: String, message: String, conversationId: Int?): AssistantChatResponse
    suspend fun getAssistantConversations(token: String): AssistantConversationsResponse
    suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse
    suspend fun deleteAssistantConversation(token: String, conversationId: Int): DefaultResponse
}
