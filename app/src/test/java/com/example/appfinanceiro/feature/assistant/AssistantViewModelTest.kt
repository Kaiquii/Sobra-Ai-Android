package com.example.appfinanceiro.feature.assistant

import com.example.appfinanceiro.MainDispatcherRule
import com.example.appfinanceiro.core.data.AssistantDataSource
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun keepsConversationAcrossMissingInformationConfirmationAndCreation() = runTest {
        val fake = FakeAssistantDataSource(listOf("needs_information", "needs_confirmation", "created"))
        var refreshes = 0
        val viewModel = AssistantViewModel(fake) { refreshes++ }
        for ((index, message) in listOf("Compra sintética", "Categoria Casa", "Confirmo").withIndex()) {
            viewModel.sendMessage("synthetic-token", message)
            advanceUntilIdle()
            assertEquals(if (index == 2) 1 else 0, refreshes)
            assertEquals(42, viewModel.uiState.value.currentConversationId)
            assertFalse(viewModel.uiState.value.isSending)
        }
        assertEquals(listOf(null, 42, 42), fake.conversationIds)
        assertEquals(6, viewModel.uiState.value.messages.size)
        assertEquals("Resposta created", viewModel.uiState.value.messages.last().content)
    }

    @Test
    fun cancellationAndLegacyRepliesDoNotInvalidateFinancialData() = runTest {
        val fake = FakeAssistantDataSource(listOf("cancelled", null))
        var refreshes = 0
        val viewModel = AssistantViewModel(fake) { refreshes++ }
        repeat(2) {
            viewModel.sendMessage("synthetic-token", "Cancelar")
            advanceUntilIdle()
        }
        assertEquals(0, refreshes)
        assertEquals(4, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onlyExpenseCreationInvalidatesAndRevisionRemainsAvailableAfterReply() = runTest {
        val initialRevision = FinanceDataChanges.revision.value
        val fake = FakeAssistantDataSource(listOf("created", "created"))
        fake.toolCall = "other_tool"
        val viewModel = AssistantViewModel(fake)
        viewModel.sendMessage("synthetic-token", "Outra operação")
        advanceUntilIdle()
        assertEquals(initialRevision, FinanceDataChanges.revision.value)
        fake.toolCall = "create_expense"
        viewModel.sendMessage("synthetic-token", "Confirmo")
        advanceUntilIdle()
        assertEquals(initialRevision + 1, FinanceDataChanges.revision.value)
    }

    private class FakeAssistantDataSource(statuses: List<String?>) : AssistantDataSource {
        private val responses = ArrayDeque(statuses.map { status ->
            createFinanceGson().fromJson(
                """{"conversation_id":42,"reply":"Resposta $status","tool_call":"create_expense",
                    "tool_result":${if (status == null) "null" else """{"status":"$status","missing_fields":["category"]}"""}}""",
                AssistantChatResponse::class.java
            )
        })
        val conversationIds = mutableListOf<Int?>()
        var toolCall = "create_expense"

        override suspend fun chatAssistant(token: String, message: String, conversationId: Int?): AssistantChatResponse {
            conversationIds += conversationId
            return responses.removeFirst().copy(tool_call = toolCall)
        }

        override suspend fun getAssistantConversations(token: String) =
            AssistantConversationsResponse(0, emptyList())

        override suspend fun getAssistantMessages(token: String, conversationId: Int) =
            AssistantMessagesResponse(AssistantConversationSummary(conversationId, "Conversa sintética"), emptyList())

        override suspend fun deleteAssistantConversation(token: String, conversationId: Int) =
            DefaultResponse("Conversa excluída")
    }
}
