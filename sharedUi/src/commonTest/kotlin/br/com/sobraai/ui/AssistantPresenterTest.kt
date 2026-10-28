package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

internal open class NativeFake : NativeFeatures {
    var shared: FinanceFile? = null
    var selected: ProfilePhoto? = null
    var partial: ((String) -> Unit)? = null
    override suspend fun pickPhoto() = selected
    override suspend fun share(file: FinanceFile) { shared = file }
    override suspend fun transcribe(onPartial: (String) -> Unit): String? { partial = onPartial; awaitCancellation() }
    override fun openLink(url: String) = Unit
    override fun cancelInteractions() = Unit
}
internal open class AssistantFake : AssistantDataSource {
    var reply = AssistantChatResponse(1, "Resposta sintética.")
    var calls = 0
    var conversationSent: Int? = null
    override suspend fun chatAssistant(token: String, message: String, conversationId: Int?): AssistantChatResponse { calls++; conversationSent = conversationId; return reply }
    override suspend fun getAssistantConversations(token: String) = AssistantConversationsResponse(0, emptyList())
    override suspend fun getAssistantMessages(token: String, conversationId: Int) = AssistantMessagesResponse(AssistantConversationSummary(conversationId, "Teste"), listOf(AssistantStoredMessage(1, conversationId, "assistant", "Histórico sintético.")))
    override suspend fun deleteAssistantConversation(token: String, conversationId: Int) = DefaultResponse("ok")
}
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantPresenterTest {
    @Test fun onlyConfirmedCreatedExpenseInvalidatesFinancialData() = runTest {
        val source = AssistantFake(); var changes = 0
        val p = AssistantPresenter(source, NativeFake(), "synthetic", backgroundScope, {}, { changes++ })
        for ((tool, status) in listOf(null to null, "create_expense" to "pending", "create_expense" to null, "create_expense" to "created")) {
            source.reply = AssistantChatResponse(1, "Resposta", tool_call = tool, tool_result = status?.let { AssistantToolResult(it) })
            p.input("Mensagem"); p.send(); p.send(); runCurrent()
        }
        assertEquals(1, changes); assertEquals(4, source.calls); assertEquals(1, source.conversationSent)
    }
    @Test fun quotaStopsNewSendsUntilServerDelayExpires() = runTest {
        var time = 1000L; val source = AssistantFake().also { it.reply = AssistantChatResponse(1, "Aguarde", error_code = "gemini_quota_exceeded", retry_after_seconds = 60) }
        val p = AssistantPresenter(source, NativeFake(), "synthetic", backgroundScope, {}, {}, { time })
        p.input("Mensagem"); p.send(); runCurrent(); p.input("Outra"); p.send(); runCurrent()
        assertEquals(1, source.calls)
        time += 60_001; p.send(); runCurrent(); assertEquals(2, source.calls)
    }
    @Test fun newAssistantErrorsAndHttpFailuresRespectServerRetryAndNeverReplayTheSend() = runTest {
        for (code in listOf("assistant_quota_exceeded", "assistant_user_limit", "assistant_unavailable")) {
            for (httpFailure in listOf(false, true)) {
                var time = 1_000L
                val source = object : AssistantFake() {
                    override suspend fun chatAssistant(token: String, message: String, conversationId: Int?): AssistantChatResponse {
                        calls++
                        if (httpFailure) throw ApiResponseException(503, "Aguarde", code, 17)
                        return AssistantChatResponse(42, "Aguarde", error_code = code, retry_after_seconds = 17)
                    }
                }
                val p = AssistantPresenter(source, NativeFake(), "synthetic", backgroundScope, {}, {}, { time })
                p.input("Mensagem"); p.send(); runCurrent()
                if (httpFailure) assertEquals("Mensagem", p.state.value.input)
                p.input("Outra mensagem"); p.send(); runCurrent()
                assertEquals(1, source.calls)
                time += 17_001; p.send(); runCurrent()
                assertEquals(2, source.calls)
            }
        }
    }
    @Test fun openingAnotherConversationClearsOldMessagesAndIgnoresLateResponse() = runTest {
        val old = CompletableDeferred<AssistantMessagesResponse>()
        val source = object : AssistantFake() { override suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse =
            if (conversationId == 1) withContext(NonCancellable) { old.await() } else super.getAssistantMessages(token, conversationId) }
        val p = AssistantPresenter(source, NativeFake(), "synthetic", backgroundScope, {}, {})
        p.open(1); runCurrent(); p.open(2); assertTrue(p.state.value.messages.isEmpty()); runCurrent()
        old.complete(AssistantMessagesResponse(AssistantConversationSummary(1, "Antiga"), listOf(AssistantStoredMessage(1, 1, "user", "Antiga")))); runCurrent()
        assertEquals(2, p.state.value.conversation); assertEquals("Histórico sintético.", p.state.value.messages.single().content)
    }
    @Test fun lateVoiceCallbackAfterStoppingCannotReplaceInputOrSendMessage() = runTest {
        val native = NativeFake(); val source = AssistantFake()
        val p = AssistantPresenter(source, native, "synthetic", backgroundScope, {}, {})
        p.voice(); runCurrent(); native.partial!!("Texto ditado")
        assertEquals("Texto ditado", p.state.value.input)
        p.stopVoice(); p.input("Texto revisado"); native.partial!!("Callback antigo"); runCurrent()
        assertEquals("Texto revisado", p.state.value.input); assertEquals(0, source.calls); assertFalse(p.state.value.listening)
    }
    @Test fun expiredSessionDoesNotFabricateReplyOrFinancialMutation() = runTest {
        var expired = 0; var changes = 0
        val source = object : AssistantFake() { override suspend fun chatAssistant(token: String, message: String, conversationId: Int?): AssistantChatResponse = throw SessionExpiredException() }
        val p = AssistantPresenter(source, NativeFake(), "synthetic", backgroundScope, { expired++ }, { changes++ })
        p.input("Mensagem"); p.send(); runCurrent()
        assertEquals(1, expired); assertEquals(0, changes); assertEquals(listOf("user"), p.state.value.messages.map { it.role })
    }
    @Test fun deletingSelectedConversationResetsContext() = runTest {
        val p = AssistantPresenter(AssistantFake(), NativeFake(), "synthetic", backgroundScope, {}, {})
        p.open(1); runCurrent(); p.delete(1); runCurrent()
        assertNull(p.state.value.conversation); assertTrue(p.state.value.messages.isEmpty())
    }
}
