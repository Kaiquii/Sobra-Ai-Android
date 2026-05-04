package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.core.network.CategoriesResponse
import com.example.appfinanceiro.core.network.ChartReportResponse
import com.example.appfinanceiro.core.network.DefaultResponse
import com.example.appfinanceiro.core.network.ExpensesResponse
import com.example.appfinanceiro.core.network.IncomesResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.core.network.YearlySummaryResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient

class FinanceRepository {

    private fun bearer(token: String): String = "Bearer $token"

    suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        return RetrofitClient.financeApi.getSummary(
            token = bearer(token),
            month = month,
            year = year
        )
    }

    suspend fun getCategories(token: String): CategoriesResponse {
        return RetrofitClient.financeApi.getCategories(bearer(token))
    }

    suspend fun getExpenses(token: String, month: Int, year: Int): ExpensesResponse {
        return RetrofitClient.financeApi.getExpenses(
            token = bearer(token),
            month = month,
            year = year
        )
    }

    suspend fun getIncomes(token: String, month: Int? = null, year: Int? = null): IncomesResponse {
        return RetrofitClient.financeApi.getIncomes(
            token = bearer(token),
            month = month,
            year = year
        )
    }

    suspend fun deleteExpense(
        token: String,
        id: Int,
        deleteFuture: Boolean? = null
    ): DefaultResponse {
        return RetrofitClient.financeApi.deleteExpense(
            token = bearer(token),
            id = id,
            deleteFuture = deleteFuture
        )
    }

    suspend fun getReportCategories(
        token: String,
        month: Int,
        year: Int
    ): List<CategoryReportResponse> {
        return RetrofitClient.financeApi.getReportCategories(
            token = bearer(token),
            month = month,
            year = year
        )
    }

    suspend fun getReportChart(token: String, year: Int): List<ChartReportResponse> {
        return RetrofitClient.financeApi.getReportChart(
            token = bearer(token),
            year = year
        )
    }

    suspend fun getYearlySummary(token: String, year: Int): YearlySummaryResponse {
        return RetrofitClient.financeApi.getYearlySummary(
            token = bearer(token),
            year = year
        )
    }
}
