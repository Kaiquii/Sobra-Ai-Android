package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.*

/** Editable financial contracts, independent of an Activity, ViewModel or HTTP engine. */
interface FinanceMutationDataSource {
    suspend fun getCategories(token: String): CategoriesResponse
    suspend fun getExpenseById(token: String, id: Int): Expense
    suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse
    suspend fun createExpense(token: String, request: ExpenseRequest): DefaultResponse
    suspend fun updateExpense(token: String, id: Int, request: ExpenseUpdateRequest): DefaultResponse
    suspend fun createIncome(token: String, request: IncomeRequest): DefaultResponse
    suspend fun updateIncome(token: String, id: Int, request: IncomeUpdateRequest): DefaultResponse
    suspend fun deleteIncome(token: String, id: Int, deleteFuture: Boolean? = null): DefaultResponse
}
