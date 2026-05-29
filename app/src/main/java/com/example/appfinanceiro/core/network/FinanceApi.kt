package com.example.appfinanceiro.core.network

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

data class SummaryResponse(
    val month: Int,
    val year: Int,
    val salario: Double,
    val adiantamento: Double,
    val renda_extra_amt: Double,
    val restante_salario: Double,
    val restante_adiantamento: Double,
    val restante_renda_extra: Double,
    val total_gasto_salario: Double,
    val total_gasto_adiantamento: Double,
    val total_gasto_renda_extra: Double,
    val total_expense: Double,
    val total_geral_disponivel: Double,
    val total_income: Double
)

data class Category(
    val id: Int,
    val user_id: Int? = null,
    val name: String
)

data class CategoriesResponse(val categories: List<Category>, val total: Int)

data class Expense(
    val id: Int,
    val category_id: Int,
    val amount: Double,
    val description: String,
    val date: String,
    val type: String,
    val installments: Int?,
    val current_installment: Int?,
    val payment_source: String?
)
data class ExpensesResponse(val expenses: List<Expense>, val total: Int)

data class Income(
    val id: Int,
    val source: String,
    val amount: Double,
    val month: Int,
    val year: Int
)
data class IncomesResponse(val incomes: List<Income>, val total: Int)

data class IncomeRequest(
    val source: String,
    val amount: Double,
    val month: Int,
    val year: Int,
    val type: String,
    val repeat_future: Boolean? = null
)

data class IncomeUpdateRequest(
    val amount: Double? = null,
    val update_future: Boolean? = null
)

data class CategoryRequest(
    val name: String
)

data class CategoryResponse(
    val data: Category,
    val message: String
)

data class AssistantChatRequest(
    val message: String,
    val conversation_id: Int? = null
)

data class AssistantChatResponse(
    val conversation_id: Int,
    val reply: String,
    val tool_call: String? = null,
    val error_code: String? = null,
    val retry_after_seconds: Int? = null
)

data class AssistantConversation(
    val conversation_id: Int,
    val title: String,
    val created_at: String,
    val updated_at: String,
    val display_date: String? = null,
    val display_time: String? = null,
    val display_label: String? = null
)

data class AssistantConversationsResponse(
    val total: Int,
    val conversations: List<AssistantConversation>
)

data class AssistantConversationSummary(
    val conversation_id: Int,
    val title: String,
    val display_label: String? = null
)

data class AssistantStoredMessage(
    val message_id: Int,
    val conversation_id: Int,
    val role: String,
    val content: String,
    val display_time: String? = null
)

data class AssistantMessagesResponse(
    val conversation: AssistantConversationSummary,
    val messages: List<AssistantStoredMessage>
)


data class ExpenseRequest(
    val amount: Double,
    val description: String,
    val category_id: Int,
    val payment_source: String,
    val date: String,
    val type: String,
    val installments: Int
)

data class DefaultResponse(
    val message: String
)

data class ExpenseUpdateRequest(
    val amount: Double? = null,
    val description: String? = null,
    val category_id: Int? = null,
    val payment_source: String? = null,
    val type: String? = null,
    val date: String? = null,
    val update_future: Boolean? = null
)

data class UpdateProfileRequest(
    val name: String,
    val email: String
)

data class CategoryReportResponse(
    val category_id: Int,
    val category_name: String,
    val total_amount: Double,
    val percentage: Double
)

data class ChartReportResponse(
    val month: Int,
    val income: Double,
    val expense: Double
)

data class YearlySummaryResponse(
    val economia_total: Double,
    val media_mensal: Double,
    val year: Int
)

interface FinanceApi {
    @retrofit2.http.POST("api/assistant/chat")
    suspend fun chatAssistant(
        @Header("Authorization") token: String,
        @retrofit2.http.Body request: AssistantChatRequest
    ): AssistantChatResponse

    @GET("api/assistant/conversations")
    suspend fun getAssistantConversations(
        @Header("Authorization") token: String
    ): AssistantConversationsResponse

    @GET("api/assistant/conversations/{id}/messages")
    suspend fun getAssistantMessages(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): AssistantMessagesResponse

    @DELETE("api/assistant/conversations/{id}")
    suspend fun deleteAssistantConversation(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): DefaultResponse

    @GET("api/reports/summary")
    suspend fun getSummary(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): SummaryResponse

    @GET("api/categories/")
    suspend fun getCategories(
        @Header("Authorization") token: String
    ): CategoriesResponse

    @GET("api/expenses")
    suspend fun getExpenses(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): ExpensesResponse

    @GET("api/incomes/")
    suspend fun getIncomes(
        @Header("Authorization") token: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): IncomesResponse

    @retrofit2.http.POST("api/expenses/")
    suspend fun createExpense(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: ExpenseRequest
    ): DefaultResponse

    @retrofit2.http.PATCH("api/expenses/{id}")
    suspend fun updateExpense(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Body request: ExpenseUpdateRequest
    ): DefaultResponse

    @retrofit2.http.DELETE("api/expenses/{id}")
    suspend fun deleteExpense(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Query("delete_future") deleteFuture: Boolean? = null
    ): DefaultResponse

    @retrofit2.http.GET("api/expenses/{id}")
    suspend fun getExpenseById(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int
    ): Expense

    @retrofit2.http.POST("api/incomes/")
    suspend fun createIncome(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: IncomeRequest
    ): DefaultResponse

    @retrofit2.http.PATCH("api/incomes/{id}")
    suspend fun updateIncome(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Body request: IncomeUpdateRequest
    ): DefaultResponse

    @DELETE("api/incomes/{id}")
    suspend fun deleteIncome(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Query("delete_future") deleteFuture: Boolean? = null
    ): DefaultResponse

    @retrofit2.http.POST("api/categories/")
    suspend fun createCategory(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: CategoryRequest
    ): CategoryResponse

    @retrofit2.http.PATCH("api/categories/{id}")
    suspend fun updateCategory(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Body request: CategoryRequest
    ): DefaultResponse

    @retrofit2.http.DELETE("api/categories/{id}")
    suspend fun deleteCategory(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int
    ): DefaultResponse

    @retrofit2.http.PATCH("api/users/profile")
    suspend fun updateProfile(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: UpdateProfileRequest
    ): DefaultResponse

    @GET("api/reports/categories")
    suspend fun getReportCategories(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): List<CategoryReportResponse>

    @GET("api/reports/chart")
    suspend fun getReportChart(
        @Header("Authorization") token: String,
        @Query("year") year: Int
    ): List<ChartReportResponse>

    @GET("api/reports/yearly-summary")
    suspend fun getYearlySummary(
        @Header("Authorization") token: String,
        @Query("year") year: Int
    ): YearlySummaryResponse
}
