package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.data.SessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class AssistantErrorsContractTest {
    @Test fun controlledChatErrorsKeepCodeDelayAndConversationWithoutReplayingThePost() = runTest {
        for (code in listOf("assistant_quota_exceeded", "assistant_user_limit", "assistant_unavailable", "gemini_quota_exceeded")) {
            var calls = 0
            val client = HttpClient(MockEngine { request ->
                calls++
                assertEquals("/api/assistant/chat", request.url.encodedPath)
                val body = FinanceJson.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertEquals("42", body["conversation_id"]?.jsonPrimitive?.content)
                assertFalse("model_id" in body)
                assertFalse("history" in body)
                respond("""{"conversation_id":42,"reply":"Aviso sintético","error_code":"$code","retry_after_seconds":17}""")
            }) { configureFinanceHttpClient() }
            try {
                val api = KtorFinanceApi(ApiTransport(client, ApiConfiguration("https://example.invalid/")))
                val response = api.chatAssistant("Bearer synthetic", AssistantChatRequest("Mensagem sintética", 42))
                assertEquals(code, response.error_code)
                assertEquals(17, assistantRetryDelaySeconds(response.error_code, response.retry_after_seconds))
                assertEquals(42, response.conversation_id)
                assertEquals(1, calls)
            } finally { client.close() }
        }
    }

    @Test fun httpErrorsKeepMetadataWithCustomPlatformExceptionFactoriesAndDoNotReplayWrites() = runTest {
        class PlatformError(status: Int, message: String?) : ApiResponseException(status, message)
        for (key in listOf("error_code", "code")) {
            var calls = 0
            val client = HttpClient(MockEngine {
                calls++
                respond("""{"error":"Serviço sintético indisponível","$key":"assistant_unavailable","retry_after_seconds":19}""",
                    HttpStatusCode.ServiceUnavailable)
            }) { configureFinanceHttpClient() }
            try {
                val transport = ApiTransport(client, ApiConfiguration("https://example.invalid/"), ::PlatformError)
                val error = assertFailsWith<PlatformError> {
                    transport.request<String>("POST", "api/assistant/chat", "Bearer synthetic", body = "{}")
                }
                assertEquals(503, error.statusCode)
                assertEquals("Serviço sintético indisponível", error.apiMessage)
                assertEquals("assistant_unavailable", error.errorCode)
                assertEquals(19, error.retryAfterSeconds)
                assertEquals(1, calls)
            } finally { client.close() }
        }
    }

    @Test fun authenticationStillExpiresTheSessionInsteadOfBecomingAQuotaError() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"error":"Sessão expirada","error_code":"assistant_quota_exceeded","retry_after_seconds":60}""",
                HttpStatusCode.Unauthorized)
        }) { configureFinanceHttpClient() }
        try {
            assertFailsWith<SessionExpiredException> {
                ApiTransport(client, ApiConfiguration("https://example.invalid/")).request<String>("POST", "api/assistant/chat", "Bearer synthetic", body = "{}")
            }
        } finally { client.close() }
    }

    @Test fun quotaRecognitionAndDelayAreProviderNeutralAndRejectInvalidOrUnrelatedRetries() {
        for (code in listOf("assistant_quota_exceeded", "assistant_user_limit", "gemini_quota_exceeded")) {
            assertTrue(isAssistantQuotaError(code))
            assertTrue(isAssistantError(code))
            assertEquals(60, assistantRetryDelaySeconds(code, 60))
        }
        assertFalse(isAssistantQuotaError("assistant_unavailable"))
        assertEquals(12, assistantRetryDelaySeconds("assistant_unavailable", 12))
        for (code in listOf(null, "other_error")) {
            assertFalse(isAssistantError(code))
            assertNull(assistantRetryDelaySeconds(code, 60))
        }
        for (delay in listOf(null, 0, -1)) assertNull(assistantRetryDelaySeconds("assistant_user_limit", delay))
        assertEquals(ApiErrorDetails(null, null, null), parseApiErrorDetails("<html>erro</html>"))
        assertNull(parseApiErrorDetails("""{"error":{},"code":"assistant_unavailable","retry_after_seconds":-3}""").retryAfterSeconds)
    }
}
