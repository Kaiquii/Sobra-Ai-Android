package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.*
import kotlinx.serialization.encodeToString

interface PatrimonyDataSource {
    suspend fun getPatrimony(token: String): PatrimonyResponse
    suspend fun updateFgts(token: String, amount: PatrimonyAmount): FgtsBalance
    suspend fun deleteFgts(token: String)
    suspend fun createInvestment(token: String, request: InvestmentWriteRequest): Investment
    suspend fun updateInvestment(token: String, id: Int, request: InvestmentWriteRequest): Investment
    suspend fun deleteInvestment(token: String, id: Int)
}

/** Shares the host's authenticated transport; never reads or mutates monthly finances. */
class SharedPatrimonyRepository(private val transport: ApiTransport) : PatrimonyDataSource, InvestmentCategoriesDataSource {
    private fun bearer(token: String): String {
        require(token.isNotBlank()) { "Sessão inválida. Entre novamente." }
        return "Bearer " + token
    }
    override suspend fun getPatrimony(token: String): PatrimonyResponse =
        transport.request("GET", "api/patrimony/", bearer(token))
    override suspend fun updateFgts(token: String, amount: PatrimonyAmount): FgtsBalance =
        transport.request<FgtsWriteResponse>("PUT", "api/patrimony/fgts", bearer(token),
            body = FinanceJson.encodeToString(FgtsWriteRequest(amount.requireEntryLimit()))).data
    override suspend fun deleteFgts(token: String) {
        transport.request<DefaultResponse>("DELETE", "api/patrimony/fgts", bearer(token))
    }
    override suspend fun createInvestment(token: String, request: InvestmentWriteRequest): Investment {
        val validated = validatedInvestment(request)
        return transport.request<InvestmentWriteResponse>("POST", "api/patrimony/investments", bearer(token),
            body = FinanceJson.encodeToString(validated)).data
    }
    override suspend fun updateInvestment(token: String, id: Int, request: InvestmentWriteRequest): Investment {
        require(id > 0)
        val validated = validatedInvestment(request)
        return transport.request<InvestmentWriteResponse>("PATCH", "api/patrimony/investments/" + id, bearer(token),
            body = FinanceJson.encodeToString(validated)).data
    }
    override suspend fun deleteInvestment(token: String, id: Int) {
        require(id > 0)
        transport.request<DefaultResponse>("DELETE", "api/patrimony/investments/" + id, bearer(token))
    }

    override suspend fun getCategories(token: String): InvestmentCategoriesResponse =
        transport.request("GET", "api/patrimony/investment-categories", bearer(token))

    override suspend fun createCategory(token: String, name: String): InvestmentCategory =
        transport.request<InvestmentCategoryWriteResponse>("POST", "api/patrimony/investment-categories", bearer(token),
            body = FinanceJson.encodeToString(investmentCategoryRequest(name))).data

    override suspend fun renameCategory(token: String, id: Int, name: String): InvestmentCategory {
        require(id > 0) { "Selecione uma categoria válida." }
        return transport.request<InvestmentCategoryWriteResponse>("PATCH", "api/patrimony/investment-categories/" + id,
            bearer(token), body = FinanceJson.encodeToString(investmentCategoryRequest(name))).data
    }

    override suspend fun deleteCategory(token: String, id: Int) {
        require(id > 0) { "Selecione uma categoria válida." }
        transport.request<DefaultResponse>("DELETE", "api/patrimony/investment-categories/" + id, bearer(token))
    }

    private fun validatedInvestment(request: InvestmentWriteRequest): InvestmentWriteRequest {
        require((request.category == null) != (request.category_id == null)) {
            "Informe apenas uma categoria, pelo nome ou pelo identificador."
        }
        val amount = request.amount.requireEntryLimit().input()
        return request.category_id?.let { investmentRequest(request.name, it, amount) }
            ?: investmentRequest(request.name, requireNotNull(request.category), amount)
    }
}
