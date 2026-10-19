package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.*



open class SharedFinanceRepository(private val api: SharedFinanceApi) : HomeDataSource, ExpensesDataSource, ReportsDataSource, AssistantDataSource, FinanceMutationDataSource, ProfileDataSource {

    private fun bearer(token: String): String = "Bearer $token"

    private suspend fun <T> authorizedRequest(block: suspend () -> T): T =
        block()

    override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        return authorizedRequest {
            api.getSummary(
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
            api.chatAssistant(
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
            api.getAssistantConversations(bearer(token))
        }
    }

    override suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse {
        return authorizedRequest {
            api.getAssistantMessages(
                token = bearer(token),
                id = conversationId
            )
        }
    }

    override suspend fun deleteAssistantConversation(token: String, conversationId: Int): DefaultResponse {
        return authorizedRequest {
            api.deleteAssistantConversation(
                token = bearer(token),
                id = conversationId
            )
        }
    }

    override suspend fun getCategories(token: String): CategoriesResponse {
        return authorizedRequest {
            api.getCategories(bearer(token))
        }
    }

    override suspend fun getExpenses(
        token: String,
        month: Int,
        year: Int,
        paymentStatus: String?
    ): ExpensesResponse {
        return authorizedRequest {
            api.getExpenses(
                token = bearer(token),
                month = month,
                year = year,
                paymentStatus = paymentStatus
            )
        }
    }

    override suspend fun getEffectiveExpenses(
        token: String,
        month: Int,
        year: Int,
        paymentStatus: String?
    ): ExpensesResponse {
        return authorizedRequest {
            api.getExpenses(
                token = bearer(token),
                month = month,
                year = year,
                paymentStatus = paymentStatus,
                periodMode = "effective"
            )
        }
    }

    override suspend fun getIncomes(token: String, month: Int?, year: Int?): IncomesResponse {
        return authorizedRequest {
            api.getIncomes(
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
            api.deleteExpense(
                token = bearer(token),
                id = id,
                deleteFuture = deleteFuture
            )
        }
    }

    override suspend fun updateExpensePaymentStatus(
        token: String,
        id: Int,
        isPaid: Boolean
    ): ExpensePaymentStatusResponse {
        return authorizedRequest {
            api.updateExpensePaymentStatus(
                token = bearer(token),
                id = id,
                request = ExpensePaymentStatusRequest(isPaid = isPaid)
            )
        }
    }

    override suspend fun updateAdvanceStatus(
        token: String,
        id: Int,
        isAdvanced: Boolean,
        advancedAt: String?
    ): AdvanceStatusResponse {
        return authorizedRequest {
            api.updateAdvanceStatus(
                token = bearer(token),
                id = id,
                request = UpdateAdvanceStatusRequest(
                    isAdvanced = isAdvanced,
                    advancedAt = advancedAt
                )
            )
        }
    }

    override suspend fun getReportCategories(
        token: String,
        month: Int,
        year: Int
    ): List<CategoryReportResponse> {
        return authorizedRequest {
            api.getReportCategories(
                token = bearer(token),
                month = month,
                year = year
            ).orEmpty()
        }
    }

    override suspend fun getReportChart(token: String, year: Int): List<ChartReportResponse> {
        return authorizedRequest {
            api.getReportChart(
                token = bearer(token),
                year = year
            )
        }
    }

    override suspend fun getYearlySummary(token: String, year: Int): YearlySummaryResponse {
        return authorizedRequest {
            api.getYearlySummary(
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
            api.getMonthComparison(
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
            api.getInstallmentCommitments(
                token = bearer(token),
                months = months,
                month = month,
                year = year,
                includeCurrentMonthAsPaid = includeCurrentMonthAsPaid
            )
        }
    }

    override suspend fun createExpense(token: String, request: ExpenseRequest): DefaultResponse {
        return authorizedRequest {
            api.createExpense(bearer(token), request)
        }
    }

    override suspend fun updateExpense(
        token: String,
        id: Int,
        request: ExpenseUpdateRequest
    ): DefaultResponse {
        return authorizedRequest {
            api.updateExpense(bearer(token), id, request)
        }
    }

    override suspend fun getExpenseById(token: String, id: Int): Expense {
        return authorizedRequest {
            api.getExpenseById(bearer(token), id)
        }
    }

    override suspend fun createIncome(token: String, request: IncomeRequest): DefaultResponse {
        return authorizedRequest {
            api.createIncome(bearer(token), request)
        }
    }

    override suspend fun updateIncome(
        token: String,
        id: Int,
        request: IncomeUpdateRequest
    ): DefaultResponse {
        return authorizedRequest {
            api.updateIncome(bearer(token), id, request)
        }
    }

    override suspend fun deleteIncome(
        token: String,
        id: Int,
        deleteFuture: Boolean?
    ): DefaultResponse {
        return authorizedRequest {
            api.deleteIncome(bearer(token), id, deleteFuture)
        }
    }

    override suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse {
        return authorizedRequest {
            api.createCategory(bearer(token), request)
        }
    }

    override suspend fun updateCategory(
        token: String,
        id: Int,
        request: CategoryRequest
    ): DefaultResponse {
        return authorizedRequest {
            api.updateCategory(bearer(token), id, request)
        }
    }

    override suspend fun deleteCategory(token: String, id: Int): DefaultResponse {
        return authorizedRequest {
            api.deleteCategory(bearer(token), id)
        }
    }

    override suspend fun getProfile(token: String): ProfileResponse {
        return authorizedRequest {
            api.getProfile(bearer(token))
        }
    }

    override suspend fun updateProfile(token: String, request: UpdateProfileRequest): DefaultResponse {
        return authorizedRequest {
            api.updateProfile(bearer(token), request)
        }
    }

    override suspend fun deleteProfilePhoto(token: String): DefaultResponse = api.deleteProfilePhoto(bearer(token))
}
