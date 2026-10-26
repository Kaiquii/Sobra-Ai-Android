package com.example.appfinanceiro.feature.patrimonio

import br.com.sobraai.ui.runtime.PresentationModel
import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class InvestmentCategoriesUiState(
    val categories: List<InvestmentCategory> = emptyList(),
    /** Only a catalog GET returning 404 enables the legacy text contract. */
    val supported: Boolean? = null,
    val loading: Boolean = false,
    val loadError: String? = null,
    val saving: Boolean = false,
    val mutationError: String? = null,
    val successMessage: String? = null,
    val reviewNotice: String? = null,
    val mutationRevision: Long = 0,
    val createdCategoryId: Int? = null,
    val isSessionExpired: Boolean = false
)

class InvestmentCategoriesViewModel(private val repository: InvestmentCategoriesDataSource) : PresentationModel() {
    private val mutable = MutableStateFlow(InvestmentCategoriesUiState())
    val state = mutable.asStateFlow()
    private var token: String? = null
    private var sessionGeneration = 0
    private var readRevision = 0L
    private var mutation: Job? = null

    fun load(sessionToken: String, retry: Boolean = false) {
        if (sessionToken.isBlank()) return
        if (token != sessionToken) {
            sessionGeneration++
            mutation?.cancel()
            token = sessionToken
            readRevision = 0
            mutable.value = InvestmentCategoriesUiState()
        }
        val generation = sessionGeneration
        // A previous 404 is a compatibility result, not a catalog to cache after publication.
        launchLatest("investment-categories", listOf(sessionToken, readRevision),
            reuseRecent = !retry && mutable.value.supported != false) {
            mutable.update { it.copy(loading = true, loadError = it.loadError.takeIf { _ -> it.supported == true }) }
            try {
                val response = repository.getCategories(sessionToken)
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) mutable.update {
                    it.copy(categories = response.categories, supported = true, loadError = null)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) expireSession()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) when {
                    error is ApiResponseException && error.statusCode == 401 -> expireSession()
                    error is ApiResponseException && error.statusCode == 404 -> mutable.update {
                        it.copy(categories = emptyList(), supported = false, loadError = null)
                    }
                    error is ApiResponseException && error.statusCode == 403 -> mutable.update {
                        it.copy(categories = emptyList(), supported = null,
                            loadError = error.userMessageOr("Acesso às categorias indisponível."))
                    }
                    else -> mutable.update {
                        it.copy(loadError = error.userMessageOr("Não foi possível carregar as categorias de investimentos."))
                    }
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration) mutable.update { it.copy(loading = false) }
            }
        }
    }

    fun create(sessionToken: String, name: String) {
        val normalized = validName(name) ?: return
        mutate(sessionToken, "Categoria criada.") { repository.createCategory(sessionToken, normalized).id }
    }

    fun rename(sessionToken: String, category: InvestmentCategory, name: String) {
        if (protected(category)) return
        val normalized = validName(name) ?: return
        mutate(sessionToken, "Categoria renomeada.") {
            repository.renameCategory(sessionToken, category.id, normalized)
            null
        }
    }

    fun delete(sessionToken: String, category: InvestmentCategory) {
        if (protected(category)) return
        mutate(sessionToken, "Categoria excluída.") {
            repository.deleteCategory(sessionToken, category.id)
            null
        }
    }

    private fun protected(category: InvestmentCategory): Boolean {
        // Check the current catalog as well, so a stale caller cannot bypass protection.
        if (!category.is_default && mutable.value.categories.none { it.id == category.id && it.is_default }) return false
        mutable.update { it.copy(mutationError = "As categorias padrão são protegidas e não podem ser alteradas ou excluídas.") }
        return true
    }

    private fun validName(input: String): String? {
        return try {
            investmentCategoryRequest(input).name
        } catch (error: IllegalArgumentException) {
            mutable.update { it.copy(mutationError = error.message) }
            null
        }
    }

    private fun mutate(sessionToken: String, message: String, block: suspend () -> Int?) {
        val current = mutable.value
        if (token != sessionToken || mutation?.isActive == true || current.saving || current.loading ||
            current.supported != true || current.loadError != null || current.isSessionExpired) return
        val generation = sessionGeneration
        mutation = viewModelScope.launch {
            mutable.update { it.copy(saving = true, mutationError = null, successMessage = null,
                reviewNotice = null, createdCategoryId = null) }
            try {
                val createdId = block()
                currentCoroutineContext().ensureActive()
                if (generation != sessionGeneration) return@launch
                mutable.update { it.copy(successMessage = message, mutationRevision = it.mutationRevision + 1,
                    createdCategoryId = createdId) }
                refresh(sessionToken)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) expireSession()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) {
                    when {
                        error is ApiResponseException && error.statusCode == 401 -> expireSession()
                        error is NetworkRequestException -> {
                            mutable.update { it.copy(reviewNotice = "Não foi possível confirmar a operação. Confira as categorias antes de tentar novamente.") }
                            refresh(sessionToken)
                        }
                        else -> {
                            mutable.update { it.copy(mutationError = error.userMessageOr("Não foi possível salvar a categoria.")) }
                            // A missing item is stale data, never evidence of an unsupported catalog.
                            if (error is ApiResponseException && error.statusCode == 404) refresh(sessionToken)
                        }
                    }
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration) mutable.update { it.copy(saving = false) }
            }
        }
    }

    private fun refresh(sessionToken: String) {
        readRevision++
        load(sessionToken, retry = true)
    }

    private fun expireSession() {
        mutable.update { it.copy(categories = emptyList(), supported = null, isSessionExpired = true,
            createdCategoryId = null, successMessage = null, reviewNotice = null) }
    }

    fun refreshAfterInvestmentChange(sessionToken: String) {
        if (token == sessionToken) refresh(sessionToken)
    }

    fun clearMutationError() { mutable.update { it.copy(mutationError = null) } }
    fun consumeSuccess() { mutable.update { it.copy(successMessage = null) } }
    fun consumeCreatedCategory() { mutable.update { it.copy(createdCategoryId = null) } }
    fun consumeReviewNotice() { mutable.update { it.copy(reviewNotice = null) } }
}
