package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import okhttp3.MultipartBody

class FinanceRepository : HomeDataSource, ExpensesDataSource, ReportsDataSource, AssistantDataSource {

    private fun bearer(token: String): String = "Bearer $token"

    private suspend fun <T> authorizedRequest(block: suspend () -> T): T =
        executeApiRequest(block = block)

    override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getSummary(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    override suspend fun chatAssistant(
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

    override suspend fun getAssistantConversations(token: String): AssistantConversationsResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getAssistantConversations(bearer(token))
        }
    }

    override suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getAssistantMessages(
                token = bearer(token),
                id = conversationId
            )
        }
    }

    override suspend fun deleteAssistantConversation(token: String, conversationId: Int): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.deleteAssistantConversation(
                token = bearer(token),
                id = conversationId
            )
        }
    }

    override suspend fun getCategories(token: String): CategoriesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getCategories(bearer(token))
        }
    }

    override suspend fun getExpenses(token: String, month: Int, year: Int): ExpensesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getExpenses(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    override suspend fun getIncomes(token: String, month: Int?, year: Int?): IncomesResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getIncomes(
                token = bearer(token),
                month = month,
                year = year
            )
        }
    }

    override suspend fun deleteExpense(
        token: String,
        id: Int,
        deleteFuture: Boolean?
    ): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.deleteExpense(
                token = bearer(token),
                id = id,
                deleteFuture = deleteFuture
            )
        }
    }

    override suspend fun getReportCategories(
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

    override suspend fun getReportChart(token: String, year: Int): List<ChartReportResponse> {
        return authorizedRequest {
            RetrofitClient.financeApi.getReportChart(
                token = bearer(token),
                year = year
            )
        }
    }

    override suspend fun getYearlySummary(token: String, year: Int): YearlySummaryResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getYearlySummary(
                token = bearer(token),
                year = year
            )
        }
    }

    override suspend fun getMonthComparison(
        token: String,
        month: Int,
        year: Int,
        compareMonth: Int?,
        compareYear: Int?
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

    override suspend fun getInstallmentCommitments(
        token: String,
        months: Int,
        month: Int?,
        year: Int?,
        includeCurrentMonthAsPaid: Boolean
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

    suspend fun createExpense(token: String, request: ExpenseRequest): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.createExpense(bearer(token), request)
        }
    }

    suspend fun updateExpense(
        token: String,
        id: Int,
        request: ExpenseUpdateRequest
    ): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.updateExpense(bearer(token), id, request)
        }
    }

    suspend fun getExpenseById(token: String, id: Int): Expense {
        return authorizedRequest {
            RetrofitClient.financeApi.getExpenseById(bearer(token), id)
        }
    }

    suspend fun createIncome(token: String, request: IncomeRequest): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.createIncome(bearer(token), request)
        }
    }

    suspend fun updateIncome(
        token: String,
        id: Int,
        request: IncomeUpdateRequest
    ): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.updateIncome(bearer(token), id, request)
        }
    }

    suspend fun deleteIncome(
        token: String,
        id: Int,
        deleteFuture: Boolean? = null
    ): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.deleteIncome(bearer(token), id, deleteFuture)
        }
    }

    suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.createCategory(bearer(token), request)
        }
    }

    suspend fun updateCategory(
        token: String,
        id: Int,
        request: CategoryRequest
    ): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.updateCategory(bearer(token), id, request)
        }
    }

    suspend fun deleteCategory(token: String, id: Int): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.deleteCategory(bearer(token), id)
        }
    }

    suspend fun getProfile(token: String): ProfileResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.getProfile(bearer(token))
        }
    }

    suspend fun updateProfile(token: String, request: UpdateProfileRequest): DefaultResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.updateProfile(bearer(token), request)
        }
    }

    suspend fun updateProfilePhoto(
        token: String,
        photo: MultipartBody.Part
    ): ProfilePhotoResponse {
        return authorizedRequest {
            RetrofitClient.financeApi.updateProfilePhoto(bearer(token), photo)
        }
    }

    suspend fun deleteProfilePhoto(token: String): DefaultResponse {
        return authorizedRequest {
            val response = RetrofitClient.financeApi.deleteProfilePhoto(bearer(token))
            if (!response.isSuccessful) {
                throw retrofit2.HttpException(response)
            }
            response.body() ?: DefaultResponse(message = "")
        }
    }
}
