package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class MutationState(val busy: Boolean = false, val error: String? = null, val completed: Boolean = false)

class MutationRunner(private val scope: CoroutineScope, private val onExpired: () -> Unit) {
    private val mutable = MutableStateFlow(MutationState())
    val state = mutable.asStateFlow()
    fun error(message: String) { if (!mutable.value.busy) mutable.value = MutationState(error = message) }
    fun run(operation: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        if (mutable.value.busy || mutable.value.completed || !scope.isActive) return
        mutable.value = MutationState(busy = true)
        scope.launch {
            try {
                operation()
                ensureActive()
                mutable.value = MutationState(completed = true)
                onSuccess()
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutable.value = MutationState(error = error.userMessageOr("Não foi possível concluir. Tente novamente.")) }
        }
    }
}

data class ExpenseEditorState(val draft: ExpenseDraft = ExpenseDraft(), val original: Expense? = null,
    val categories: List<Category> = emptyList(), val loading: Boolean = true, val loadError: String? = null,
    val categoryBusy: Boolean = false, val categoryError: String? = null)

class ExpenseEditor(private val repository: FinanceMutationDataSource, private val token: String,
    private val scope: CoroutineScope, private val id: Int?, private val onExpired: () -> Unit, onSaved: () -> Unit,
    initialDate: FinancialDate = today()) {
    private val mutable = MutableStateFlow(ExpenseEditorState(draft = ExpenseDraft(date = initialDate.display)))
    val state = mutable.asStateFlow()
    val mutation = MutationRunner(scope, onExpired)
    private val saved = onSaved
    private var loadJob: Job? = null
    fun update(transform: (ExpenseDraft) -> ExpenseDraft) {
        if (!mutation.state.value.busy && !mutable.value.loading && !mutable.value.categoryBusy) mutable.update { it.copy(draft = transform(it.draft)) }
    }
    fun load() {
        if (mutable.value.loading && loadJob?.isActive == true) return
        mutable.update { it.copy(loading = true) }
        loadJob = scope.launch {
            try {
                val loaded = coroutineScope {
                    val categories = async { repository.getCategories(token).categories }
                    val expense = async { id?.let { repository.getExpenseById(token, it) } }
                    categories.await() to expense.await()
                }
                ensureActive()
                mutable.update { it.copy(categories = loaded.first, original = loaded.second,
                    draft = loaded.second?.let(ExpenseDraft::from) ?: it.draft.copy(categoryId = it.draft.categoryId ?: loaded.first.firstOrNull()?.id),
                    loading = false, loadError = null) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutable.update { it.copy(loading = false, loadError = error.userMessageOr("Não foi possível carregar o formulário.")) } }
        }
    }
    fun createCategory(name: String, onCreated: () -> Unit) {
        if (mutable.value.categoryBusy || mutation.state.value.busy || mutable.value.loading) return
        if (name.isBlank()) { mutable.update { it.copy(categoryError = "Digite um nome para a categoria.") }; return }
        mutable.update { it.copy(categoryBusy = true, categoryError = null) }
        scope.launch {
            try {
                val category = repository.createCategory(token, CategoryRequest(name.trim())).data
                ensureActive()
                mutable.update { it.copy(categories = (it.categories + category).distinctBy { c -> c.id },
                    draft = it.draft.copy(categoryId = category.id), categoryBusy = false) }
                onCreated()
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutable.update { it.copy(categoryBusy = false, categoryError = error.userMessageOr("Não foi possível criar a categoria.")) } }
        }
    }
    fun save(confirmedFuture: Boolean = false) {
        val state = mutable.value
        if (state.loading || state.loadError != null || state.categoryBusy) return
        state.draft.validate(editing = id != null)?.let { mutation.error(it); return }
        if (state.categories.none { it.id == state.draft.categoryId }) { mutation.error("Selecione uma categoria disponível."); return }
        if (state.original?.isRecurring == true && state.draft.updateFuture && !confirmedFuture) {
            mutation.error("Confirme a atualização das ocorrências futuras."); return
        }
        mutation.run({
            if (id == null) repository.createExpense(token, state.draft.createRequest())
            else repository.updateExpense(token, id, state.draft.updateRequest(requireNotNull(state.original)))
        }, saved)
    }
}

class IncomeEditor(private val repository: FinanceMutationDataSource, private val token: String, scope: CoroutineScope,
    draft: IncomeDraft, onExpired: () -> Unit, private val onSaved: () -> Unit) {
    private val mutable = MutableStateFlow(draft)
    val state = mutable.asStateFlow()
    val mutation = MutationRunner(scope, onExpired)
    fun update(transform: (IncomeDraft) -> IncomeDraft) { if (!mutation.state.value.busy) mutable.update(transform) }
    fun save(confirmedFuture: Boolean = false) {
        val draft = mutable.value
        if (parseAmount(draft.amount) == null) { mutation.error("Informe um valor maior que zero."); return }
        if ((draft.existing != null && draft.updateFuture || draft.existing == null && draft.repeatFuture) && !confirmedFuture) {
            mutation.error("Confirme a aplicação aos próximos meses."); return
        }
        mutation.run({
            if (draft.existing == null) repository.createIncome(token, draft.createRequest())
            else repository.updateIncome(token, draft.existing.id, draft.updateRequest())
        }, onSaved)
    }
    fun delete(confirmed: Boolean) {
        val draft = mutable.value
        if (!confirmed || draft.existing == null) return
        mutation.run({ repository.deleteIncome(token, draft.existing.id, if (draft.deleteFuture) true else null) }, onSaved)
    }
}
