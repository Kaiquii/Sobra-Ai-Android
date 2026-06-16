package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.core.network.CategoriesResponse
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.core.network.AssistantChatRequest
import com.example.appfinanceiro.core.network.AssistantChatResponse
import com.example.appfinanceiro.core.network.AssistantConversationsResponse
import com.example.appfinanceiro.core.network.AssistantMessagesResponse
import com.example.appfinanceiro.core.network.DefaultResponse
import com.example.appfinanceiro.core.network.ExpensesResponse
import com.example.appfinanceiro.core.network.InstallmentCommitmentsResponse
import com.example.appfinanceiro.core.network.IncomesResponse
import com.example.appfinanceiro.core.network.MonthComparisonResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.network.YearlySummaryResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import retrofit2.HttpException

class FinanceRepository {

    private fun bearer(token: String): String = "Bearer $token"

    private suspend fun <T> authorizedRequest(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException()
            }
            throw e
        }
    }

    suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getSummary(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    suspend fun chatAssistant(
        token: String,
        message: String,
        conversationId: Int?
    ): AssistantChatResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.chatAssistant(
                token = bearer(token),
                request = AssistantChatRequest(
                    message = message,
                    conversation_id = conversationId
                )
            )
        }
    }

    suspend fun getAssistantConversations(token: String): AssistantConversationsResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getAssistantConversations(bearer(token))
        }
    }

    suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getAssistantMessages(
                token = bearer(token),
                id = conversationId
            )
        }
    }

    suspend fun deleteAssistantConversation(token: String, conversationId: Int): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.deleteAssistantConversation(
                token = bearer(token),
                id = conversationId
            )
        }
    }

    suspend fun getCategories(token: String): CategoriesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getCategories(bearer(token))
        }
    }

    suspend fun getExpenses(token: String, month: Int, year: Int): ExpensesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getExpenses(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    suspend fun getIncomes(token: String, month: Int? = null, year: Int? = null): IncomesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getIncomes(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    suspend fun deleteExpense(
        token: String,
        id: Int,
        deleteFuture: Boolean? = null
    ): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.deleteExpense(
                token = bearer(token),
                id = id,
                deleteFuture = deleteFuture
            )
        }
    }

    suspend fun getReportCategories(
        token: String,
        month: Int,
        year: Int
    ): List<CategoryReportResponse> {
        return authorizedRequest {
            RetrofitClient.financeApi.getReportCategories(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    suspend fun getReportChart(token: String, year: Int): List<ChartReportResponse> {
        return authorizedRequest {
            RetrofitClient.financeApi.getReportChart(
                token = bearer(token),
                year = year
            )
        }
    }

    suspend fun getYearlySummary(token: String, year: Int): YearlySummaryResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getYearlySummary(
                token = bearer(token),
                year = year
            )
        }
    }

    suspend fun getMonthComparison(
        token: String,
        month: Int,
        year: Int,
        compareMonth: Int? = null,
        compareYear: Int? = null
    ): MonthComparisonResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getMonthComparison(
                token = bearer(token),
                month = month,
                year = year,
                compareMonth = compareMonth,
                compareYear = compareYear
            )
        }
    }

    suspend fun getInstallmentCommitments(
        token: String,
        months: Int = 12,
        month: Int? = null,
        year: Int? = null,
        includeCurrentMonthAsPaid: Boolean = false
    ): InstallmentCommitmentsResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getInstallmentCommitments(
                token = bearer(token),
                months = months,
                month = month,
                year = year,
                includeCurrentMonthAsPaid = includeCurrentMonthAsPaid
            )
        }
    }
}
