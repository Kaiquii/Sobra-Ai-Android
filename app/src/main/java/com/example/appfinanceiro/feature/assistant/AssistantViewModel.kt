package com.example.appfinanceiro.feature.assistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanceiro.core.data.FinanceRepository
import com.example.appfinanceiro.core.data.AssistantDataSource
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.AssistantConversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class AssistantMessage(
    val role: String,
    val content: String,
    val displayTime: String? = null,
    val errorCode: String? = null,
    val retryAfterSeconds: Int? = null
)

data class AssistantUiState(
    val currentConversationId: Int? = null,
    val conversations: List<AssistantConversation> = emptyList(),
    val messages: List<AssistantMessage> = emptyList(),
    val isSending: Boolean = false,
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val quotaRetrySeconds: Int? = null,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false
)

class AssistantViewModel(
    private val repository: AssistantDataSource = FinanceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState
    private var quotaCountdownJob: Job? = null

    fun sendMessage(token: String, message: String) {
        val cleanMessage = message.trim()
        if (
            cleanMessage.isBlank() ||
            _uiState.value.isSending ||
            (_uiState.value.quotaRetrySeconds ?: 0) > 0
        ) return

        val conversationId = _uiState.value.currentConversationId

        _uiState.update {
            it.copy(
                messages = it.messages + AssistantMessage("user", cleanMessage),
                isSending = true,
                errorMessage = null,
                isSessionExpired = false
            )
        }

        viewModelScope.launch {
            try {
                val response = repository.chatAssistant(
                    token = token,
                    message = cleanMessage,
                    conversationId = conversationId
                )

                _uiState.update {
                    it.copy(
                        currentConversationId = response.conversation_id,
                        messages = it.messages + AssistantMessage(
                            role = "assistant",
                            content = response.reply,
                            errorCode = response.error_code,
                            retryAfterSeconds = response.retry_after_seconds
                        ),
                        isSending = false
                    )
                }

                if (response.error_code == "gemini_quota_exceeded") {
                    startQuotaCountdown(response.retry_after_seconds)
                }

                loadConversations(token)
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSending = false, isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("ASSISTANT_ERRO", "Falha ao enviar mensagem", e)
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = e.userMessageOr("Nao foi possivel falar com o assistente.")
                    )
                }
            }
        }
    }

    private fun startQuotaCountdown(seconds: Int?) {
        val initialSeconds = seconds ?: return
        if (initialSeconds <= 0) return

        quotaCountdownJob?.cancel()
        quotaCountdownJob = viewModelScope.launch {
            var remainingSeconds = initialSeconds
            while (remainingSeconds > 0) {
                _uiState.update { it.copy(quotaRetrySeconds = remainingSeconds) }
                delay(1_000)
                remainingSeconds--
            }
            _uiState.update { it.copy(quotaRetrySeconds = null) }
        }
    }

    fun loadConversations(token: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingConversations = true, errorMessage = null)
            }

            try {
                val response = repository.getAssistantConversations(token)
                _uiState.update {
                    it.copy(
                        conversations = response.conversations,
                        isLoadingConversations = false
                    )
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isLoadingConversations = false, isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("ASSISTANT_ERRO", "Falha ao carregar conversas", e)
                _uiState.update {
                    it.copy(
                        isLoadingConversations = false,
                        errorMessage = e.userMessageOr("Não foi possível carregar as conversas.")
                    )
                }
            }
        }
    }

    fun openConversation(token: String, conversationId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentConversationId = conversationId,
                    isLoadingMessages = true,
                    errorMessage = null
                )
            }

            try {
                val response = repository.getAssistantMessages(token, conversationId)
                _uiState.update {
                    it.copy(
                        messages = response.messages.map { message ->
                            AssistantMessage(
                                role = message.role,
                                content = message.content,
                                displayTime = message.display_time
                            )
                        },
                        isLoadingMessages = false
                    )
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isLoadingMessages = false, isSessionExpired = true)
                }
            } catch (e: Exception) {
                Log.e("ASSISTANT_ERRO", "Falha ao abrir conversa", e)
                _uiState.update {
                    it.copy(
                        isLoadingMessages = false,
                        errorMessage = e.userMessageOr("Não foi possível abrir essa conversa.")
                    )
                }
            }
        }
    }

    fun deleteConversation(token: String, conversationId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteAssistantConversation(token, conversationId)
                _uiState.update {
                    it.copy(
                        conversations = it.conversations.filter { conversation ->
                            conversation.conversation_id != conversationId
                        },
                        currentConversationId = if (it.currentConversationId == conversationId) {
                            null
                        } else {
                            it.currentConversationId
                        },
                        messages = if (it.currentConversationId == conversationId) {
                            emptyList()
                        } else {
                            it.messages
                        }
                    )
                }
            } catch (e: SessionExpiredException) {
                _uiState.update { it.copy(isSessionExpired = true) }
            } catch (e: Exception) {
                Log.e("ASSISTANT_ERRO", "Falha ao apagar conversa", e)
                _uiState.update {
                    it.copy(errorMessage = e.userMessageOr("Não foi possível apagar essa conversa."))
                }
            }
        }
    }

    fun startNewConversation() {
        _uiState.update {
            it.copy(
                currentConversationId = null,
                messages = emptyList(),
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSessionExpired() {
        _uiState.update { it.copy(isSessionExpired = false) }
    }
}
