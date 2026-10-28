package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.SharedFinanceRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantAutomaticRoutingHttpTest {
    @Test
    fun newAndContinuingChatSendOnlyUserMessageAndConversationWithoutModelSetup() = runTest {
        val bodies = mutableListOf<Set<String>>()
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/base/api/assistant/chat", request.url.encodedPath)
            assertEquals("Bearer synthetic", request.headers[HttpHeaders.Authorization])
            assertTrue(request.url.parameters.isEmpty())
            assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
            val body = FinanceJson.parseToJsonElement((request.body as TextContent).text).jsonObject
            bodies += body.keys
            if (bodies.size == 1) {
                assertEquals(setOf("message"), body.keys)
                assertEquals("Primeira mensagem", body.getValue("message").jsonPrimitive.content)
            } else {
                assertEquals(setOf("message", "conversation_id"), body.keys)
                assertEquals("Segunda mensagem", body.getValue("message").jsonPrimitive.content)
                assertEquals("41", body.getValue("conversation_id").jsonPrimitive.content)
            }
            // Routing metadata may change on the server without a new client preference.
            respond("""{"conversation_id":41,"reply":"Resposta sintética","routing_mode":"automatic"}""")
        }) { configureFinanceHttpClient() }
        try {
            val repository = SharedFinanceRepository(KtorFinanceApi(ApiTransport(client,
                ApiConfiguration("https://example.invalid/base/"))))
            val first = repository.chatAssistant("synthetic", "Primeira mensagem", null)
            val second = repository.chatAssistant("synthetic", "Segunda mensagem", first.conversation_id)
            assertEquals(41, first.conversation_id)
            assertEquals(first.conversation_id, second.conversation_id)
            assertEquals("Resposta sintética", second.reply)
            assertEquals(2, bodies.size)
        } finally { client.close() }
    }
}
