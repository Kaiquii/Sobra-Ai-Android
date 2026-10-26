package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.InvestmentCategoriesResponse
import com.example.appfinanceiro.core.network.InvestmentCategory

/** Catálogo próprio de investimentos, independente das categorias de despesas. */
interface InvestmentCategoriesDataSource {
    suspend fun getCategories(token: String): InvestmentCategoriesResponse
    suspend fun createCategory(token: String, name: String): InvestmentCategory
    suspend fun renameCategory(token: String, id: Int, name: String): InvestmentCategory
    suspend fun deleteCategory(token: String, id: Int)
}
