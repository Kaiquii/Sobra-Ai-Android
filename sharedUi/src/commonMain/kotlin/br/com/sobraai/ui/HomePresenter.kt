package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.HomeDataSource
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.date.shiftMonth
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Period(val month: Int, val year: Int) {
    init { require(month in 1..12) }
    fun shift(delta: Int): Period = shiftMonth(month - 1, year, delta).let { Period(it.monthIndex + 1, it.year) }
    val label: String get() = "${MONTHS[month - 1]} $year"
}

private val MONTHS = listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")

data class HomeSnapshot(
    val period: Period,
    val summary: SummaryResponse,
    val incomes: List<Income>,
    val expenses: List<Expense>,
    val categories: Map<Int, String>
)

data class HomeState(
    val period: Period,
    val snapshot: HomeSnapshot? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val category: Int? = null,
    val source: String? = null,
    val status: String? = null
) {
    val filteredExpenses: List<Expense> get() = snapshot?.expenses.orEmpty().filter { expense ->
        (category == null || expense.category_id == category) &&
            (source == null || expense.paymentSources().any { normalizeSource(it) == normalizeSource(source) }) &&
            (status == null || expense.is_paid == (status == "paid"))
    }
}

private fun normalizeSource(value: String) = value.lowercase().replace("á", "a")

/** One atomic snapshot per period. Cancellation and generation both prevent stale responses. */
class HomePresenter(
    private val repository: HomeDataSource,
    private val token: String,
    private val scope: CoroutineScope,
    initialPeriod: Period,
    private val onExpired: () -> Unit
) {
    private val mutable = MutableStateFlow(HomeState(initialPeriod))
    val state = mutable.asStateFlow()
    private var request: Job? = null
    private var generation = 0

    fun selectPeriod(period: Period) {
        mutable.value = mutable.value.copy(period = period)
        refresh()
    }

    fun filter(category: Int?, source: String?, status: String?) {
        require(status == null || status == "paid" || status == "pending")
        mutable.value = mutable.value.copy(category = category, source = source, status = status)
    }

    fun refresh() {
        request?.cancel()
        val version = ++generation
        val period = mutable.value.period
        mutable.value = mutable.value.copy(loading = true)
        request = scope.launch {
            try {
                val snapshot = coroutineScope {
                    val summary = async { repository.getSummary(token, period.month, period.year) }
                    val incomes = async { repository.getIncomes(token, period.month, period.year) }
                    val categories = async { repository.getCategories(token) }
                    // Load all statuses once; filters operate on this same effective-period snapshot.
                    val expenses = async { repository.getEffectiveExpenses(token, period.month, period.year, null) }
                    val incomeList = incomes.await().incomes.filter { it.month == period.month && it.year == period.year }
                    val summaryData = summary.await()
                    require(summaryData.month == period.month && summaryData.year == period.year) { "O resumo recebido não corresponde ao período solicitado." }
                    HomeSnapshot(period, summaryData.copy(
                        salario = incomeList.filter { normalizeSource(it.source) == "salario" }.sumOf { it.amount },
                        adiantamento = incomeList.filter { normalizeSource(it.source) == "adiantamento" }.sumOf { it.amount },
                        renda_extra_amt = incomeList.filter { normalizeSource(it.source) == "renda extra" }.sumOf { it.amount }
                    ), incomeList, expenses.await().expenses, categories.await().categories.associate { it.id to it.name })
                }
                if (version == generation) mutable.value = mutable.value.copy(snapshot = snapshot, loading = false, error = null)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: SessionExpiredException) {
                if (version == generation) { clear(); onExpired() }
            } catch (error: Exception) {
                if (version == generation) mutable.value = mutable.value.copy(loading = false,
                    error = error.userMessageOr("Não foi possível carregar os dados. Tente novamente."))
            }
        }
    }

    fun clear() {
        generation++
        request?.cancel()
        mutable.value = HomeState(mutable.value.period)
    }
}
