package com.example.appfinanceiro.core.data

import androidx.lifecycle.ViewModel
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.core.network.SessionAccessEvents
import okhttp3.MultipartBody

class FinanceActionsViewModel(
    private val repository: FinanceRepository = FinanceRepository()
) : ViewModel() {
    private suspend fun <T> request(block: suspend () -> T): T {
        return try {
            block()
        } catch (error: SessionExpiredException) {
            SessionAccessEvents.notifyAccessRevoked(
                "Sua sessão expirou. Faça login novamente."
            )
            throw error
        }
    }

    suspend fun getCategories(token: String): CategoriesResponse =
        request { repository.getCategories(token) }

    suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse =
        this.request { repository.createCategory(token, request) }

    suspend fun updateCategory(token: String, id: Int, request: CategoryRequest): DefaultResponse =
        this.request { repository.updateCategory(token, id, request) }

    suspend fun deleteCategory(token: String, id: Int): DefaultResponse =
        request { repository.deleteCategory(token, id) }

    suspend fun getExpense(token: String, id: Int): Expense =
        request { repository.getExpenseById(token, id) }

    suspend fun createExpense(token: String, expense: ExpenseRequest): DefaultResponse =
        request { repository.createExpense(token, expense) }

    suspend fun updateExpense(
        token: String,
        id: Int,
        expense: ExpenseUpdateRequest
    ): DefaultResponse = request { repository.updateExpense(token, id, expense) }

    suspend fun getIncomes(
        token: String,
        month: Int? = null,
        year: Int? = null
    ): IncomesResponse = request { repository.getIncomes(token, month, year) }

    suspend fun createIncome(token: String, income: IncomeRequest): DefaultResponse =
        request { repository.createIncome(token, income) }

    suspend fun updateIncome(
        token: String,
        id: Int,
        income: IncomeUpdateRequest
    ): DefaultResponse = request { repository.updateIncome(token, id, income) }

    suspend fun deleteIncome(
        token: String,
        id: Int,
        deleteFuture: Boolean? = null
    ): DefaultResponse = request { repository.deleteIncome(token, id, deleteFuture) }

    suspend fun getProfile(token: String): ProfileResponse = request { repository.getProfile(token) }

    suspend fun updateProfile(token: String, profile: UpdateProfileRequest): DefaultResponse =
        request { repository.updateProfile(token, profile) }

    suspend fun updateProfilePhoto(
        token: String,
        photo: MultipartBody.Part
    ): ProfilePhotoResponse = request { repository.updateProfilePhoto(token, photo) }

    suspend fun deleteProfilePhoto(token: String): DefaultResponse =
        request { repository.deleteProfilePhoto(token) }
}
