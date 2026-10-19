package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*

internal fun testExpense(id: Int = 1, type: String = "Fixa", date: String = "2026-10-20T00:00:00-03:00",
    advanced: Boolean = false, advancedAt: String? = null, amount: Double = 100.0) = Expense(id, 1, amount,
    "Conta sintética", date, type, if (type == "Parcelada") 6 else null, if (type == "Parcelada") 2 else null,
    "Salário", isAdvanced = advanced, advancedAt = advancedAt)

internal class EditingFake : HomeDataSource, ExpensesDataSource, FinanceMutationDataSource {
    var categories = listOf(Category(1, name = "Casa"), Category(2, name = "Saúde"))
    var incomes = listOf(Income(1, "Salario", 1000.0, 10, 2026))
    var expense = testExpense()
    var scheduled = listOf(expense)
    var effective = listOf(expense)
    var readFailure: Exception? = null
    var writeFailure: Exception? = null
    var beforeWrite: suspend () -> Unit = {}
    var beforeRead: suspend (Int) -> Unit = {}
    var summaryReads = 0
    var scheduledReads = 0
    var effectiveReads = 0
    var writes = 0
    var createdExpense: ExpenseRequest? = null
    var updatedExpense: Pair<Int, ExpenseUpdateRequest>? = null
    var createdIncome: IncomeRequest? = null
    var updatedIncome: Pair<Int, IncomeUpdateRequest>? = null
    var deletedIncome: Pair<Int, Boolean?>? = null
    var deletedExpense: Pair<Int, Boolean?>? = null
    var paid: Pair<Int, Boolean>? = null
    var advanced: Triple<Int, Boolean, String?>? = null

    private suspend fun writing() { writes++; beforeWrite(); writeFailure?.let { throw it } }
    override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse {
        summaryReads++; beforeRead(month); readFailure?.let { throw it }
        return SummaryResponse(month, year, 1000.0, 0.0, 0.0, 900.0, 0.0, 0.0, 100.0, 0.0, 0.0, 100.0, 900.0, 1000.0)
    }
    override suspend fun getIncomes(token: String, month: Int?, year: Int?) = IncomesResponse(incomes.filter { (month == null || it.month == month) && (year == null || it.year == year) }, incomes.size)
    override suspend fun getCategories(token: String): CategoriesResponse { readFailure?.let { throw it }; return CategoriesResponse(categories, categories.size) }
    override suspend fun getExpenseById(token: String, id: Int): Expense { readFailure?.let { throw it }; return expense.copy(id = id) }
    override suspend fun getExpenses(token: String, month: Int, year: Int, paymentStatus: String?): ExpensesResponse {
        scheduledReads++; beforeRead(month); readFailure?.let { throw it }; return ExpensesResponse(scheduled, scheduled.size)
    }
    override suspend fun getEffectiveExpenses(token: String, month: Int, year: Int, paymentStatus: String?): ExpensesResponse {
        effectiveReads++; beforeRead(month); readFailure?.let { throw it }; return ExpensesResponse(effective, effective.size)
    }
    override suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse {
        writing(); val category = Category(3, name = request.name); categories = categories + category; return CategoryResponse(category, "Criada")
    }
    override suspend fun createExpense(token: String, request: ExpenseRequest): DefaultResponse { writing(); createdExpense = request; return DefaultResponse("Criada") }
    override suspend fun updateExpense(token: String, id: Int, request: ExpenseUpdateRequest): DefaultResponse { writing(); updatedExpense = id to request; return DefaultResponse("Atualizada") }
    override suspend fun createIncome(token: String, request: IncomeRequest): DefaultResponse { writing(); createdIncome = request; return DefaultResponse("Criada") }
    override suspend fun updateIncome(token: String, id: Int, request: IncomeUpdateRequest): DefaultResponse { writing(); updatedIncome = id to request; return DefaultResponse("Atualizada") }
    override suspend fun deleteIncome(token: String, id: Int, deleteFuture: Boolean?): DefaultResponse { writing(); deletedIncome = id to deleteFuture; return DefaultResponse("Excluída") }
    override suspend fun deleteExpense(token: String, id: Int, deleteFuture: Boolean?): DefaultResponse { writing(); deletedExpense = id to deleteFuture; return DefaultResponse("Excluída") }
    override suspend fun updateExpensePaymentStatus(token: String, id: Int, isPaid: Boolean): ExpensePaymentStatusResponse {
        writing(); paid = id to isPaid; return ExpensePaymentStatusResponse("Atualizada", expense.copy(is_paid = isPaid))
    }
    override suspend fun updateAdvanceStatus(token: String, id: Int, isAdvanced: Boolean, advancedAt: String?): AdvanceStatusResponse {
        writing(); advanced = Triple(id, isAdvanced, advancedAt); return AdvanceStatusResponse("Atualizada", expense.copy(isAdvanced = isAdvanced, advancedAt = advancedAt))
    }
}
