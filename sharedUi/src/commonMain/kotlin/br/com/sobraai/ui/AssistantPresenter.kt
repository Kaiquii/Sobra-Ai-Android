package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

data class ChatMessage(val role: String, val content: String, val displayTime: String? = null)
data class ChatState(val conversation: Int? = null, val messages: List<ChatMessage> = emptyList(),
    val conversations: List<AssistantConversation> = emptyList(), val input: String = "", val sending: Boolean = false,
    val loading: Boolean = false, val historyLoading: Boolean = false, val error: String? = null, val historyError: String? = null,
    val retryAt: Long = 0, val listening: Boolean = false)

class AssistantPresenter(private val source: AssistantDataSource, private val native: NativeFeatures, private val token: String,
    private val scope: CoroutineScope, private val onExpired: () -> Unit, private val onExpenseCreated: () -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {
    private val mutable = MutableStateFlow(ChatState())
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    private var voiceJob: Job? = null
    private var voiceRevision = 0
    private var revision = 0
    val deletion = FeatureAction(scope, onExpired)
    fun input(text: String) { if (!mutable.value.sending) mutable.update { it.copy(input = text) } }
    fun history() {
        if (mutable.value.historyLoading) return
        mutable.update { it.copy(historyLoading = true) }
        scope.launch {
            try { val list = source.getAssistantConversations(token).conversations; ensureActive(); mutable.update { it.copy(conversations = list, historyLoading = false, historyError = null) } }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutable.update { it.copy(historyLoading = false, historyError = error.userMessageOr("Não foi possível carregar as conversas.")) } }
        }
    }
    fun open(id: Int?) {
        if (mutable.value.sending) return
        stopVoice(); loadJob?.cancel(); val version = ++revision
        // A previous conversation is never presented under the new conversation ID.
        mutable.update { it.copy(conversation = id, messages = emptyList(), input = "", loading = id != null, error = null) }
        if (id == null) return
        loadJob = scope.launch {
            try {
                val response = source.getAssistantMessages(token, id); ensureActive()
                require(response.conversation.conversation_id == id)
                if (version == revision) mutable.update { it.copy(messages = response.messages.map { m -> ChatMessage(m.role, m.content, m.display_time) }, loading = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { if (version == revision) mutable.update { it.copy(loading = false, error = error.userMessageOr("Não foi possível abrir a conversa.")) } }
        }
    }
    fun send() {
        val state = mutable.value
        val message = state.input.trim()
        if (state.sending || state.loading || message.isBlank() || now() < state.retryAt) return
        stopVoice()
        mutable.update { it.copy(sending = true, error = null, input = "", messages = it.messages + ChatMessage("user", message)) }
        scope.launch {
            try {
                val response = source.chatAssistant(token, message, state.conversation); ensureActive()
                if (response.tool_call == "create_expense" && response.tool_result?.status == "created") onExpenseCreated()
                mutable.update { it.copy(conversation = response.conversation_id, sending = false,
                    messages = it.messages + ChatMessage("assistant", response.reply),
                    retryAt = if (response.error_code == "gemini_quota_exceeded" && (response.retry_after_seconds ?: 0) > 0)
                        now() + response.retry_after_seconds!!.toLong() * 1000 else it.retryAt) }
                history()
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutable.update { it.copy(sending = false, input = message,
                error = error.userMessageOr("Não foi possível confirmar o envio. Confira a conversa antes de reenviar.")) } }
        }
    }
    fun delete(id: Int) {
        if (mutable.value.sending) return
        deletion.run { source.deleteAssistantConversation(token, id); if (mutable.value.conversation == id) open(null); history(); "Conversa excluída." }
    }
    fun voice() {
        if (mutable.value.sending || mutable.value.loading || mutable.value.listening) return
        val version = ++voiceRevision
        mutable.update { it.copy(listening = true, error = null) }
        voiceJob = scope.launch {
            try {
                val result = native.transcribe { partial -> if (version == voiceRevision && scope.isActive) mutable.update { it.copy(input = partial) } }; ensureActive()
                if (version == voiceRevision) mutable.update { it.copy(listening = false, input = result ?: it.input) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { mutable.update { it.copy(listening = false, error = error.userMessageOr("Não foi possível usar a entrada por voz.")) } }
        }
    }
    fun stopVoice() { voiceRevision++; voiceJob?.cancel(); voiceJob = null; mutable.update { it.copy(listening = false) } }
}
