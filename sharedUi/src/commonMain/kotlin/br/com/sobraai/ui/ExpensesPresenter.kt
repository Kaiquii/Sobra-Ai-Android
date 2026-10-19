package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ExpenseFilters(val search: String = "", val type: String = "Todas", val category: Int? = null,
    val source: String? = null, val status: String? = null) {
    fun apply(expenses: List<Expense>): List<Expense> {
        val query = search.trim()
        val amountQuery = normalizeAmountSearch(query)
        return expenses.filter { expense ->
            (query.isBlank() || expense.description.contains(query, true) ||
                (amountQuery.isNotBlank() && (normalizeAmountSearch(formatCurrency(expense.amount)).contains(amountQuery) ||
                    normalizeAmountSearch(expense.amount.toString()).contains(amountQuery)))) &&
                (type == "Todas" || when (type) {
                    "Parceladas" -> expense.type.equals("Parcelada", true)
                    "Fixas" -> expense.type.equals("Fixa", true)
                    "Únicas" -> expense.type.equals("Única", true) || expense.type.equals("Unica", true)
                    else -> false
                }) && (category == null || category == expense.category_id) &&
                (source == null || expense.paymentSources().any { sourceKey(it) == sourceKey(source) }) &&
                when (status) { "paid" -> expense.is_paid; "pending" -> !expense.is_paid; else -> true }
        }
    }
}
private fun normalizeAmountSearch(value: String) = value.lowercase().replace("r$", "").filter { it.isDigit() || it == ',' || it == '.' }.replace(',', '.')

data class ExpensesSnapshot(val period: Period, val scheduled: List<Expense>, val effective: List<Expense>, val categories: Map<Int, String>) {
    val incoming: List<Expense> get() = effective.filter { expense ->
        expense.isAdvanced && FinancialDate.parseApi(expense.date)?.let { it.month != period.month || it.year != period.year } == true
    }.distinctBy { it.id }
    val categoryOptions: Map<Int, String> get() = categories.filterKeys { id -> (scheduled + incoming).any { it.category_id == id } }
}
data class ExpensesState(val period: Period, val snapshot: ExpensesSnapshot? = null, val filters: ExpenseFilters = ExpenseFilters(),
    val loading: Boolean = false, val error: String? = null) {
    val scheduled: List<Expense> get() = filters.apply(snapshot?.scheduled.orEmpty())
    val incoming: List<Expense> get() = filters.apply(snapshot?.incoming.orEmpty())
    val total: Double get() = filters.apply(snapshot?.effective.orEmpty().distinctBy { it.id }).sumOf { it.amount }
}

class ExpensesPresenter(private val repository: ExpensesDataSource, private val token: String, private val scope: CoroutineScope,
    period: Period, private val onExpired: () -> Unit) {
    private val mutable = MutableStateFlow(ExpensesState(period))
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    fun selectPeriod(period: Period) { mutable.update { it.copy(period = period) }; refresh() }
    fun filter(filters: ExpenseFilters) { mutable.update { it.copy(filters = filters) } }
    fun refresh() {
        job?.cancel()
        val version = ++generation
        val period = mutable.value.period
        mutable.update { it.copy(loading = true) }
        job = scope.launch {
            try {
                val data = coroutineScope {
                    val categories = async { repository.getCategories(token) }
                    val scheduled = async { repository.getExpenses(token, period.month, period.year, null) }
                    val effective = async { repository.getEffectiveExpenses(token, period.month, period.year, null) }
                    ExpensesSnapshot(period, scheduled.await().expenses, effective.await().expenses,
                        categories.await().categories.associate { it.id to it.name })
                }
                if (version == generation) mutable.update { it.copy(snapshot = data, loading = false, error = null) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { if (version == generation) { clear(); onExpired() } }
            catch (error: Exception) { if (version == generation) mutable.update { it.copy(loading = false, error = error.userMessageOr("Não foi possível carregar as despesas.")) } }
        }
    }
    fun clear() { ++generation; job?.cancel(); mutable.value = ExpensesState(mutable.value.period) }
}
