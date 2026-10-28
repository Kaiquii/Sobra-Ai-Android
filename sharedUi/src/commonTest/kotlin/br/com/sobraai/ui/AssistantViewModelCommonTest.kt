package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.assistant.AssistantViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

private open class AssistantChatStateFake : AssistantDataSource {
    var reply = AssistantChatResponse(42, "Resposta sintética")
    var failure: Exception? = null
    var calls = 0
    var histories = 0
    var deleted = 0
    val conversationIds = mutableListOf<Int?>()
    var beforeChat: suspend () -> Unit = {}
    override suspend fun chatAssistant(token: String, message: String, conversationId: Int?): AssistantChatResponse {
        calls++; conversationIds += conversationId; beforeChat(); failure?.let { throw it }; return reply
    }
    override suspend fun getAssistantConversations(token: String): AssistantConversationsResponse {
        histories++; return AssistantConversationsResponse(0, emptyList())
    }
    override suspend fun getAssistantMessages(token: String, conversationId: Int) = AssistantMessagesResponse(
        AssistantConversationSummary(conversationId, "Conversa sintética"),
        listOf(AssistantStoredMessage(1, conversationId, "assistant", "Histórico $conversationId")))
    override suspend fun deleteAssistantConversation(token: String, conversationId: Int): DefaultResponse {
        deleted++; return DefaultResponse("Conversa excluída")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelCommonTest {
    @Test fun responseErrorsFromAnyAssistantProviderRespectPositiveRetryAndNeverReplaySends() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (code in listOf("assistant_quota_exceeded", "assistant_user_limit", "assistant_unavailable", "gemini_quota_exceeded")) {
                val source = AssistantChatStateFake().apply {
                    reply = reply.copy(error_code = code, retry_after_seconds = 5)
                }
                val model = AssistantViewModel(source)
                try {
                    assertTrue(model.sendMessage("synthetic", "Mensagem")); runCurrent()
                    assertEquals(5, model.uiState.value.quotaRetrySeconds)
                    assertEquals(code, model.uiState.value.messages.last().errorCode)
                    assertFalse(model.sendMessage("synthetic", "Bloqueada")); runCurrent()
                    assertEquals(1, source.calls)
                    advanceTimeBy(4_000); runCurrent()
                    assertEquals(1, model.uiState.value.quotaRetrySeconds)
                    model.startNewConversation()
                    assertEquals(1, model.uiState.value.quotaRetrySeconds)
                    advanceTimeBy(1_000); runCurrent()
                    assertNull(model.uiState.value.quotaRetrySeconds)
                    source.reply = source.reply.copy(error_code = null, retry_after_seconds = null)
                    assertTrue(model.sendMessage("synthetic", "Agora permitida")); runCurrent()
                    assertEquals(2, source.calls)
                } finally { model.close() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun typedHttpAssistantErrorsUseServerRetryWithoutFabricatingAnAssistantReply() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (code in listOf("assistant_quota_exceeded", "assistant_user_limit", "assistant_unavailable", "gemini_quota_exceeded")) {
                val source = AssistantChatStateFake().apply { failure = ApiResponseException(503, "Aguarde o servidor.", code, 3) }
                val model = AssistantViewModel(source)
                try {
                    assertTrue(model.sendMessage("synthetic", "Mensagem")); runCurrent()
                    assertEquals(3, model.uiState.value.quotaRetrySeconds)
                    assertEquals("Aguarde o servidor.", model.uiState.value.errorMessage)
                    assertEquals(listOf("user"), model.uiState.value.messages.map { it.role })
                    assertFalse(model.sendMessage("synthetic", "Bloqueada"))
                    assertEquals(1, source.calls)
                    source.failure = null
                    advanceTimeBy(3_000); runCurrent()
                    assertTrue(model.sendMessage("synthetic", "Permitida")); runCurrent()
                    assertEquals(2, source.calls)
                } finally { model.close() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun missingNonpositiveOrUnrelatedRetryMetadataDoesNotStartCooldown() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for ((code, retry) in listOf("assistant_unavailable" to null, "assistant_user_limit" to 0,
                "assistant_quota_exceeded" to -5, "unrelated_error" to 90)) {
                val source = AssistantChatStateFake().apply { reply = reply.copy(error_code = code, retry_after_seconds = retry) }
                val model = AssistantViewModel(source)
                try {
                    model.sendMessage("synthetic", "Mensagem"); runCurrent()
                    assertNull(model.uiState.value.quotaRetrySeconds)
                    assertTrue(model.sendMessage("synthetic", "Nova tentativa explícita")); runCurrent()
                    assertEquals(2, source.calls)
                } finally { model.close() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun acceptedSendIsSynchronousAndBlocksDuplicateAndContextChangesUntilCompletion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = AssistantChatStateFake()
        val model = AssistantViewModel(source)
        val gate = CompletableDeferred<Unit>()
        try {
            model.openConversation("synthetic", 42); runCurrent()
            source.beforeChat = { gate.await() }
            assertTrue(model.sendMessage("synthetic", "  Mensagem  "))
            assertTrue(model.uiState.value.isSending)
            assertFalse(model.sendMessage("synthetic", "Outra"))
            model.startNewConversation()
            model.openConversation("synthetic", 99)
            model.deleteConversation("synthetic", 42)
            runCurrent()
            assertEquals(1, source.calls)
            assertEquals(0, source.deleted)
            assertEquals(42, model.uiState.value.currentConversationId)
            assertEquals("Mensagem", model.uiState.value.messages.last().content)
            gate.complete(Unit); runCurrent()
            assertFalse(model.uiState.value.isSending)
            assertEquals(listOf<Int?>(42), source.conversationIds)
            assertEquals("Resposta sintética", model.uiState.value.messages.last().content)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun loadingConversationPreventsSendingAgainstIncompleteContext() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val source = object : AssistantChatStateFake() {
            override suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse {
                gate.await(); return super.getAssistantMessages(token, conversationId)
            }
        }
        val model = AssistantViewModel(source)
        try {
            model.openConversation("synthetic", 42); runCurrent()
            assertFalse(model.sendMessage("synthetic", "Rascunho mantido pela UI"))
            assertEquals(0, source.calls)
            gate.complete(Unit); runCurrent()
            assertTrue(model.sendMessage("synthetic", "Rascunho mantido pela UI")); runCurrent()
            assertEquals(1, source.calls)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun lateReplyFromPreviousAccountCannotPublishOrInvalidateMonthlyData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val source = AssistantChatStateFake().apply {
            beforeChat = { withContext(NonCancellable) { gate.await() } }
            reply = reply.copy(tool_call = "create_expense", tool_result = AssistantToolResult("created"))
        }
        var changes = 0
        val model = AssistantViewModel(source) { changes++ }
        try {
            model.sendMessage("old", "Mensagem antiga"); runCurrent()
            model.bindSession("new")
            assertTrue(model.uiState.value.messages.isEmpty())
            assertFalse(model.uiState.value.isSending)
            gate.complete(Unit); runCurrent()
            assertTrue(model.uiState.value.messages.isEmpty())
            assertNull(model.uiState.value.currentConversationId)
            assertEquals(0, changes)
            assertEquals(0, source.histories)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun openingAnotherConversationDiscardsLateMessagesAndLogoutDiscardsHistory() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val messages = CompletableDeferred<Unit>()
        val history = CompletableDeferred<Unit>()
        val source = object : AssistantChatStateFake() {
            override suspend fun getAssistantMessages(token: String, conversationId: Int): AssistantMessagesResponse {
                if (conversationId == 1) withContext(NonCancellable) { messages.await() }
                return super.getAssistantMessages(token, conversationId)
            }
            override suspend fun getAssistantConversations(token: String): AssistantConversationsResponse {
                withContext(NonCancellable) { history.await() }
                return AssistantConversationsResponse(1, listOf(AssistantConversation(2, "Antiga", "", "")))
            }
        }
        val model = AssistantViewModel(source)
        try {
            model.openConversation("synthetic", 1); runCurrent()
            model.openConversation("synthetic", 2); runCurrent()
            assertEquals("Histórico 2", model.uiState.value.messages.single().content)
            messages.complete(Unit); runCurrent()
            assertEquals(2, model.uiState.value.currentConversationId)
            assertEquals("Histórico 2", model.uiState.value.messages.single().content)
            model.loadConversations("synthetic"); runCurrent()
            model.bindSession(null)
            history.complete(Unit); runCurrent()
            assertTrue(model.uiState.value.conversations.isEmpty())
            assertTrue(model.uiState.value.messages.isEmpty())
        } finally { messages.complete(Unit); history.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun expiredSessionClearsConversationAndCooldownWhileCancellationIsNotReportedAsError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = AssistantChatStateFake()
        val model = AssistantViewModel(source)
        try {
            model.openConversation("synthetic", 42); runCurrent()
            source.failure = CancellationException("Cancelamento sintético")
            model.sendMessage("synthetic", "Mensagem"); runCurrent()
            assertNull(model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.isSending)
            source.failure = SessionExpiredException()
            model.sendMessage("synthetic", "Mensagem"); runCurrent()
            assertTrue(model.uiState.value.isSessionExpired)
            assertTrue(model.uiState.value.messages.isEmpty())
            assertNull(model.uiState.value.currentConversationId)
            assertNull(model.uiState.value.quotaRetrySeconds)
            assertFalse(model.sendMessage("synthetic", "Bloqueada"))
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun actualLogoutThenSameTokenLoginReloadsHistoryInsteadOfReusingOldSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = object : AssistantChatStateFake() {
            override suspend fun getAssistantConversations(token: String): AssistantConversationsResponse {
                histories++
                return AssistantConversationsResponse(1, listOf(AssistantConversation(histories, "Conversa sintética", "", "")))
            }
        }
        val model = AssistantViewModel(source)
        try {
            model.loadConversations("synthetic"); runCurrent()
            assertEquals(1, model.uiState.value.conversations.single().conversation_id)
            model.bindSession(null)
            assertTrue(model.uiState.value.conversations.isEmpty())
            model.loadConversations("synthetic"); runCurrent()
            assertEquals(2, source.histories)
            assertEquals(2, model.uiState.value.conversations.single().conversation_id)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun automaticChatContinuesWithoutModelPreferenceSetupAndRespectsServerQuota() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val chatSource = AssistantChatStateFake()
        val chat = AssistantViewModel(chatSource)
        try {
            assertTrue(chat.sendMessage("synthetic", "Primeira mensagem")); runCurrent()
            chatSource.reply = chatSource.reply.copy(reply = "Segunda resposta sintética")
            assertTrue(chat.sendMessage("synthetic", "Continuar a conversa")); runCurrent()
            assertEquals(listOf<Int?>(null, 42), chatSource.conversationIds)
            assertEquals(listOf("Primeira mensagem", "Resposta sintética", "Continuar a conversa", "Segunda resposta sintética"),
                chat.uiState.value.messages.map { it.content })
            assertEquals(42, chat.uiState.value.currentConversationId)

            chatSource.reply = chatSource.reply.copy(error_code = "assistant_user_limit", retry_after_seconds = 30)
            assertTrue(chat.sendMessage("synthetic", "Outra mensagem")); runCurrent()
            assertEquals(30, chat.uiState.value.quotaRetrySeconds)
            assertFalse(chat.sendMessage("synthetic", "Bloqueada pelo servidor"))
            assertEquals(3, chatSource.calls)
            assertEquals(42, chat.uiState.value.currentConversationId)
        } finally { chat.close(); Dispatchers.resetMain() }
    }
}
