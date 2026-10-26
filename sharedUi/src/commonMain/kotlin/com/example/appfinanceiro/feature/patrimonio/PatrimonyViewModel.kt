package com.example.appfinanceiro.feature.patrimonio

import br.com.sobraai.ui.runtime.PresentationModel
import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PatrimonyUiState(
    val data: PatrimonyResponse? = null,
    val loading: Boolean = false,
    val loadError: String? = null,
    val saving: Boolean = false,
    val mutationError: String? = null,
    val successMessage: String? = null,
    val reviewNotice: String? = null,
    val mutationRevision: Long = 0,
    val isSessionExpired: Boolean = false
)

class PatrimonyViewModel(private val repository: PatrimonyDataSource) : PresentationModel() {
    private val mutable = MutableStateFlow(PatrimonyUiState())
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
            mutable.value = PatrimonyUiState()
        }
        val generation = sessionGeneration
        launchLatest("patrimony", listOf(sessionToken, readRevision), reuseRecent = !retry) {
            mutable.update { it.copy(loading = true, loadError = it.loadError.takeIf { _ -> it.data != null }) }
            try {
                val response = repository.getPatrimony(sessionToken)
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) mutable.update { it.copy(data = response, loadError = null) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) mutable.update { it.copy(data = null, isSessionExpired = true) }
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) mutable.update {
                    it.copy(data = it.data.takeUnless { error is ApiResponseException && error.statusCode == 403 },
                        loadError = error.userMessageOr("Não foi possível carregar seu patrimônio."))
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration) mutable.update { it.copy(loading = false) }
            }
        }
    }

    fun saveFgts(sessionToken: String, input: String) {
        val amount = parsePatrimonyAmount(input)
        if (amount == null) {
            mutable.update { it.copy(mutationError = "Informe um saldo válido, de zero a R$ 9.999.999.999,99, com até duas casas decimais.") }
            return
        }
        mutate(sessionToken, "Saldo do FGTS atualizado.") { repository.updateFgts(sessionToken, amount) }
    }

    fun saveInvestment(sessionToken: String, id: Int?, name: String, category: String, amount: String, categoryId: Int? = null) {
        val request = try {
            if (categoryId != null) investmentRequest(name, categoryId, amount) else investmentRequest(name, category, amount)
        }
        catch (error: IllegalArgumentException) {
            mutable.update { it.copy(mutationError = error.message) }; return
        }
        mutate(sessionToken, if (id == null) "Investimento cadastrado." else "Investimento atualizado.") {
            if (id == null) repository.createInvestment(sessionToken, request)
            else repository.updateInvestment(sessionToken, id, request)
        }
    }

    fun deleteFgts(sessionToken: String) = mutate(sessionToken, "Saldo do FGTS removido.") {
        repository.deleteFgts(sessionToken)
    }
    fun deleteInvestment(sessionToken: String, id: Int) = mutate(sessionToken, "Investimento excluído.") {
        repository.deleteInvestment(sessionToken, id)
    }

    private fun mutate(sessionToken: String, message: String, block: suspend () -> Unit) {
        if (token != sessionToken || mutation?.isActive == true || mutable.value.saving || mutable.value.loading ||
            mutable.value.data == null || mutable.value.loadError != null || mutable.value.isSessionExpired) return
        val generation = sessionGeneration
        mutation = viewModelScope.launch {
            mutable.update { it.copy(saving = true, mutationError = null, successMessage = null, reviewNotice = null) }
            try {
                block()
                currentCoroutineContext().ensureActive()
                if (generation != sessionGeneration) return@launch
                mutable.update { it.copy(successMessage = message, mutationRevision = it.mutationRevision + 1) }
                readRevision++
                load(sessionToken, retry = true)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) mutable.update { it.copy(data = null, isSessionExpired = true) }
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) {
                    val reconcile = error is NetworkRequestException || (error is ApiResponseException && error.statusCode == 404)
                    val notice = if (error is NetworkRequestException)
                        "Não foi possível confirmar a operação. Confira os saldos antes de tentar novamente."
                        else error.userMessageOr("Não foi possível salvar a alteração.")
                    mutable.update { it.copy(data = it.data.takeUnless { error is ApiResponseException && error.statusCode == 403 },
                        mutationError = notice.takeUnless { reconcile }, reviewNotice = notice.takeIf { reconcile }) }
                    if (reconcile) {
                        readRevision++
                        load(sessionToken, retry = true)
                    }
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration) mutable.update { it.copy(saving = false) }
            }
        }
    }

    fun refreshAfterCategoryChange(sessionToken: String) {
        if (token != sessionToken) return
        readRevision++
        load(sessionToken, retry = true)
    }

    fun clearMutationError() { mutable.update { it.copy(mutationError = null) } }
    fun consumeReviewNotice() { mutable.update { it.copy(reviewNotice = null) } }
    fun consumeSuccess() { mutable.update { it.copy(successMessage = null) } }
}
