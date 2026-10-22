package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class FinanceRoute { Home, Incomes, Expenses, ExpenseEditor, Reports, Commitments, Assistant, Profile, Categories, Help }

/** Every signed-in session owns a child scope; lock/logout cancels readers and writers. */
class FinanceWorkspace(val home: HomePresenter, private val source: ExpensesDataSource,
    private val edits: FinanceMutationDataSource, private val token: String, parentScope: CoroutineScope,
    featureFactory: ((CoroutineScope, () -> Unit) -> WorkspaceFeatures)? = null,
    private val onExpired: () -> Unit) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    val expenses = ExpensesPresenter(source, token, scope, home.state.value.period, onExpired)
    private val mutableRoute = MutableStateFlow(FinanceRoute.Home)
    val route = mutableRoute.asStateFlow()
    var expenseEditor: ExpenseEditor? = null
        private set
    private var editorJob: Job? = null
    private val mutableIncome = MutableStateFlow<IncomeEditor?>(null)
    val income = mutableIncome.asStateFlow()
    private var incomeJob: Job? = null
    private val mutableAction = MutableStateFlow<MutationRunner?>(null)
    val action = mutableAction.asStateFlow()
    val features = featureFactory?.invoke(scope, ::refreshAll)

    fun navigate(route: FinanceRoute) {
        require(route != FinanceRoute.ExpenseEditor)
        if (expenseEditor?.mutation?.state?.value?.busy == true || expenseEditor?.state?.value?.categoryBusy == true) return
        editorJob?.cancel(); expenseEditor = null
        mutableRoute.value = route
        features?.open(route)
        if (route == FinanceRoute.Expenses && expenses.state.value.snapshot == null) expenses.refresh()
    }
    fun editExpense(id: Int? = null) {
        editorJob?.cancel()
        val job = SupervisorJob(scope.coroutineContext[Job]); editorJob = job
        val period = if (route.value == FinanceRoute.Expenses) expenses.state.value.period else home.state.value.period
        val date = today().takeIf { it.month == period.month && it.year == period.year } ?: FinancialDate(period.year, period.month, 1)
        expenseEditor = ExpenseEditor(edits, token, CoroutineScope(scope.coroutineContext + job), id, onExpired, {
            refreshAll(); navigate(FinanceRoute.Expenses)
        }, date).also { it.load() }
        mutableRoute.value = FinanceRoute.ExpenseEditor
    }
    var deletingIncome: Boolean = false
        private set
    fun editIncome(source: String, existing: Income?, period: Period, deleting: Boolean = false) {
        if (mutableIncome.value != null) return
        val job = SupervisorJob(scope.coroutineContext[Job]); incomeJob = job
        deletingIncome = deleting
        mutableIncome.value = IncomeEditor(edits, token, CoroutineScope(scope.coroutineContext + job),
            IncomeDraft(if (sourceKey(source) == "salario") "Salario" else source, period, existing), onExpired) { refreshAll(); dismissIncome() }
    }
    fun dismissIncome() {
        if (mutableIncome.value?.mutation?.state?.value?.busy == true) return
        incomeJob?.cancel(); mutableIncome.value = null
    }
    fun incomeSettingsEditor(source: String, existing: Income?, period: Period, formScope: CoroutineScope): IncomeEditor =
        IncomeEditor(edits, token, formScope, IncomeDraft(source, period, existing), onExpired, ::refreshAll)
    fun newAction(): MutationRunner {
        mutableAction.value?.takeIf { it.state.value.busy }?.let { return it }
        return MutationRunner(scope, onExpired).also { mutableAction.value = it }
    }
    fun deleteExpense(expense: Expense, future: Boolean, runner: MutationRunner) {
        // Fixed expenses already remove future occurrences with the existing null/default contract.
        runner.run({ source.deleteExpense(token, expense.id, if (future && expense.type.equals("Parcelada", true)) true else null) }, ::refreshAll)
    }
    fun setPaid(expense: Expense, paid: Boolean, runner: MutationRunner) {
        runner.run({ source.updateExpensePaymentStatus(token, expense.id, paid) }, ::refreshAll)
    }
    fun advance(expense: Expense, date: FinancialDate?, runner: MutationRunner) {
        if (date != null) {
            val scheduled = FinancialDate.parseApi(expense.date)
            if (scheduled == null || date >= scheduled) { runner.error("O adiantamento deve ser anterior à data prevista da despesa."); return }
        }
        runner.run({ source.updateAdvanceStatus(token, expense.id, date != null, date?.iso) }, ::refreshAll)
    }
    fun refreshAll() { home.refresh(); expenses.refresh(); features?.invalidate() }
    fun close() { scope.cancel(); features?.native?.cancelInteractions(); expenses.clear(); mutableIncome.value = null; expenseEditor = null; mutableAction.value = null }
}
