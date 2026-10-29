package com.example.appfinanceiro.feature.assistant

import br.com.sobraai.ui.runtime.PresentationModel as ViewModel
import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.data.AssistantDataSource
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.AssistantConversation
import com.example.appfinanceiro.core.network.assistantRetryDelaySeconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AssistantUiState(
    val currentConversationId: Int? = null,
    val conversations: List<AssistantConversation> = emptyList(),
    val messages: List<AssistantMessage> = emptyList(),
    val isSending: Boolean = false,
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val conversationToDelete: AssistantConversation? = null,
    val isDeletingConversation: Boolean = false,
    val deletionErrorMessage: String? = null,
    val quotaRetrySeconds: Int? = null,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false
)

class AssistantViewModel(
    private val repository: AssistantDataSource,
    private val onExpenseCreated: () -> Unit = FinanceDataChanges::notifyExpenseCreated
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState
    private var token: String? = null
    private var sessionGeneration = 0
    private var conversationRevision = 0
    private var historyRevision = 0L
    private var quotaCountdownJob: Job? = null
    private var chatJob: Job? = null
    private var historyJob: Job? = null
    private var messagesJob: Job? = null
    private val deletions = mutableMapOf<Int, Job>()

    /** Session changes cancel every operation before another account can see its result. */
    fun bindSession(sessionToken: String?) {
        val next = sessionToken?.takeIf { it.isNotBlank() }
        if (token == next) return
        sessionGeneration++
        conversationRevision++
        chatJob?.cancel()
        historyJob?.cancel()
        messagesJob?.cancel()
        quotaCountdownJob?.cancel()
        deletions.values.forEach { it.cancel() }
        deletions.clear()
        historyRevision = 0
        token = next
        _uiState.value = AssistantUiState()
    }

    fun sendMessage(token: String, message: String): Boolean {
        if (token.isBlank()) return false
        bindSession(token)
        val cleanMessage = message.trim()
        val current = _uiState.value
        if (cleanMessage.isBlank() || current.isSending || current.isLoadingMessages || current.isDeletingConversation ||
            current.isSessionExpired || (current.quotaRetrySeconds ?: 0) > 0) return false
        val generation = sessionGeneration
        val revision = conversationRevision
        val conversationId = current.currentConversationId
        _uiState.update { it.copy(messages = it.messages + AssistantMessage("user", cleanMessage),
            isSending = true, errorMessage = null) }
        chatJob = viewModelScope.launch {
            try {
                val response = repository.chatAssistant(token, cleanMessage, conversationId)
                currentCoroutineContext().ensureActive()
                if (generation != sessionGeneration || revision != conversationRevision) return@launch
                if (response.tool_call == "create_expense" && response.tool_result?.status == "created") onExpenseCreated()
                _uiState.update { it.copy(currentConversationId = response.conversation_id,
                    messages = it.messages + AssistantMessage("assistant", response.reply,
                        errorCode = response.error_code, retryAfterSeconds = response.retry_after_seconds),
                    isSending = false) }
                startQuotaCountdown(assistantRetryDelaySeconds(response.error_code, response.retry_after_seconds), generation)
                historyRevision++
                loadConversations(token)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration && revision == conversationRevision) {
                    if (error.isExpired()) expireSession()
                    else {
                        _uiState.update { it.copy(errorMessage = error.userMessageOr("Não foi possível falar com o assistente.")) }
                        if (error is ApiResponseException) startQuotaCountdown(
                            assistantRetryDelaySeconds(error.errorCode, error.retryAfterSeconds), generation)
                    }
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration && revision == conversationRevision)
                    _uiState.update { it.copy(isSending = false) }
            }
        }
        return true
    }

    private fun startQuotaCountdown(seconds: Int?, generation: Int) {
        val initialSeconds = seconds ?: return
        quotaCountdownJob?.cancel()
        // Set before launching so a rapid second send cannot slip past the cooldown.
        _uiState.update { it.copy(quotaRetrySeconds = initialSeconds) }
        quotaCountdownJob = viewModelScope.launch {
            var remainingSeconds = initialSeconds
            while (remainingSeconds > 0) {
                delay(1_000)
                currentCoroutineContext().ensureActive()
                if (generation != sessionGeneration) return@launch
                remainingSeconds--
                _uiState.update { it.copy(quotaRetrySeconds = remainingSeconds.takeIf { value -> value > 0 }) }
            }
        }
    }

    fun loadConversations(token: String, retry: Boolean = false) {
        if (token.isBlank()) return
        bindSession(token)
        if (_uiState.value.isSessionExpired) return
        val generation = sessionGeneration
        historyJob = launchLatest("assistant-history", listOf(token, generation, historyRevision), reuseRecent = !retry) {
            _uiState.update { it.copy(isLoadingConversations = true) }
            try {
                val response = repository.getAssistantConversations(token)
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) _uiState.update { it.copy(conversations = response.conversations) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) {
                    if (error.isExpired()) expireSession()
                    else _uiState.update { it.copy(errorMessage = error.userMessageOr("Não foi possível carregar as conversas.")) }
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration)
                    _uiState.update { it.copy(isLoadingConversations = false) }
            }
        }
    }

    fun openConversation(token: String, conversationId: Int) {
        if (token.isBlank() || conversationId <= 0) return
        bindSession(token)
        if (_uiState.value.isSending || _uiState.value.isDeletingConversation || _uiState.value.isSessionExpired) return
        messagesJob?.cancel()
        val generation = sessionGeneration
        val revision = ++conversationRevision
        _uiState.update { it.copy(currentConversationId = conversationId, messages = emptyList(),
            isLoadingMessages = true, errorMessage = null, conversationToDelete = null, deletionErrorMessage = null) }
        messagesJob = viewModelScope.launch {
            try {
                val response = repository.getAssistantMessages(token, conversationId)
                currentCoroutineContext().ensureActive()
                require(response.conversation.conversation_id == conversationId) { "Conversa retornada não corresponde à solicitada." }
                if (generation == sessionGeneration && revision == conversationRevision) _uiState.update {
                    it.copy(messages = response.messages.map { message ->
                        AssistantMessage(message.role, message.content, displayTime = message.display_time)
                    })
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration && revision == conversationRevision) {
                    if (error.isExpired()) expireSession()
                    else _uiState.update { it.copy(errorMessage = error.userMessageOr("Não foi possível abrir essa conversa.")) }
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration && revision == conversationRevision)
                    _uiState.update { it.copy(isLoadingMessages = false) }
            }
        }
    }

    fun requestDeleteConversation(conversationId: Int) {
        val state = _uiState.value
        if (conversationId <= 0 || token == null || state.isSending || state.isDeletingConversation || state.isSessionExpired) return
        val conversation = state.conversations.firstOrNull { it.conversation_id == conversationId } ?: return
        _uiState.update { it.copy(conversationToDelete = conversation, deletionErrorMessage = null) }
    }

    fun cancelDeleteConversation() {
        if (_uiState.value.isDeletingConversation) return
        _uiState.update { it.copy(conversationToDelete = null, deletionErrorMessage = null) }
    }

    fun confirmDeleteConversation(token: String) {
        // A confirmation belongs to the session and item that opened it.
        if (token.isBlank() || this.token != token) return
        val conversationId = _uiState.value.conversationToDelete?.conversation_id ?: return
        if (_uiState.value.isSending || _uiState.value.isSessionExpired || deletions[conversationId]?.isActive == true) return
        val generation = sessionGeneration
        _uiState.update { it.copy(isDeletingConversation = true, deletionErrorMessage = null) }
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                repository.deleteAssistantConversation(token, conversationId)
                currentCoroutineContext().ensureActive()
                if (generation != sessionGeneration) return@launch
                _uiState.update { it.copy(conversations = it.conversations.filter { conversation -> conversation.conversation_id != conversationId },
                    conversationToDelete = null, isDeletingConversation = false, deletionErrorMessage = null) }
                if (_uiState.value.currentConversationId == conversationId) startNewConversation()
                historyRevision++
                loadConversations(token)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) {
                    if (error.isExpired()) expireSession()
                    else _uiState.update { it.copy(deletionErrorMessage = error.userMessageOr("Não foi possível apagar essa conversa.")) }
                }
            } finally {
                if (generation == sessionGeneration) {
                    deletions.remove(conversationId)
                    _uiState.update { it.copy(isDeletingConversation = false) }
                }
            }
        }
        deletions[conversationId] = job
        job.start()
    }

    fun startNewConversation() {
        if (_uiState.value.isSending || _uiState.value.isDeletingConversation) return
        conversationRevision++
        messagesJob?.cancel()
        _uiState.update { it.copy(currentConversationId = null, messages = emptyList(), isLoadingMessages = false,
            errorMessage = null, conversationToDelete = null, deletionErrorMessage = null) }
    }

    private fun Exception.isExpired() = this is SessionExpiredException || (this is ApiResponseException && statusCode == 401)
    private fun expireSession() {
        sessionGeneration++
        conversationRevision++
        quotaCountdownJob?.cancel()
        historyJob?.cancel()
        messagesJob?.cancel()
        _uiState.value = AssistantUiState(isSessionExpired = true)
    }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearSessionExpired() { _uiState.update { it.copy(isSessionExpired = false) } }
}
