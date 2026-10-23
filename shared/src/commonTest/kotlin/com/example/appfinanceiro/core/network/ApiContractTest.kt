package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlin.test.*

class ApiContractTest {
    private val token = "synthetic-token"
    private val expense = """{"id":10,"category_id":4,"amount":200.25,"description":null,"date":null,"type":null,"payment_splits":[null,{"payment_source":null,"amount":200.25}],"is_paid":true,"is_advanced":true,"advanced_at":"2026-10-01"}"""
    private val summary = """{"month":10,"year":2026,"salario":2000.0,"adiantamento":0.0,"renda_extra_amt":0.0,"restante_salario":1799.75,"restante_adiantamento":0.0,"restante_renda_extra":0.0,"total_gasto_salario":200.25,"total_gasto_adiantamento":0.0,"total_gasto_renda_extra":0.0,"total_expense":200.25,"total_geral_disponivel":1799.75,"total_income":2000.0}"""

    private suspend fun withTransport(handler: MockRequestHandler, block: suspend (ApiTransport) -> Unit) {
        val client = HttpClient(MockEngine(handler)) { configureFinanceHttpClient() }
        try { block(ApiTransport(client, ApiConfiguration("https://example.invalid/base/"))) }
        finally { client.close() }
    }

    @Test
    fun disconnectedReadRetriesOnceWithTheSameCredentialsAndQuery() = runTest {
        var calls = 0
        withTransport({ request ->
            calls++
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
            assertEquals("10", request.url.parameters["month"])
            assertEquals("effective", request.url.parameters["period_mode"])
            if (calls == 1) throw IOException("synthetic-closed-connection")
            respond("""{"expenses":[],"total":0}""")
        }) { transport ->
            assertTrue(SharedFinanceRepository(KtorFinanceApi(transport))
                .getEffectiveExpenses(token, 10, 2026, null).expenses.isEmpty())
        }
        assertEquals(2, calls)
    }

    @Test
    fun persistentConnectionFailureStopsAfterTwoReadAttempts() = runTest {
        var calls = 0
        withTransport({ calls++; throw IOException("synthetic-offline") }) { transport ->
            assertFailsWith<NetworkRequestException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
        }
        assertEquals(2, calls)
    }

    @Test
    fun failedWritesIncludingLoginAreNeverAutomaticallyResubmitted() = runTest {
        for (method in listOf("POST", "PUT", "PATCH", "DELETE")) {
            var calls = 0
            withTransport({ calls++; throw IOException("synthetic-connection-lost-after-send") }) { transport ->
                assertFailsWith<NetworkRequestException> {
                    transport.request<DefaultResponse>(method, "api/synthetic", "Bearer $token", body = "{}")
                }
            }
            assertEquals(1, calls, method)
        }
    }

    @Test
    fun serverRejectionsAndMalformedResponsesDoNotTriggerConnectionRetry() = runTest {
        for (status in listOf(401, 403, 429, 500, 503)) {
            var calls = 0
            withTransport({ calls++; respond("{}", HttpStatusCode.fromValue(status)) }) { transport ->
                assertFailsWith<ApiResponseException> {
                    transport.request<DefaultResponse>("GET", "api/synthetic", authenticated = false)
                }
            }
            assertEquals(1, calls, "HTTP $status")
        }
        var calls = 0
        withTransport({ calls++; respond("invalid-json") }) { transport ->
            assertFailsWith<SerializationException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
        }
        assertEquals(1, calls)
    }

    @Test
    fun configuredTimeoutIsNotExtendedByAnExtraAttempt() = runTest {
        var calls = 0
        withTransport({ request -> calls++; throw HttpRequestTimeoutException(request) }) { transport ->
            assertFailsWith<NetworkRequestException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
        }
        assertEquals(1, calls)
    }

    @Test
    fun cancellationAfterConnectionFailurePreventsAnotherRead() = runTest {
        var calls = 0
        val failed = CompletableDeferred<Unit>()
        withTransport({ calls++; failed.complete(Unit); throw IOException("synthetic-closed-connection") }) { transport ->
            val request = launch { KtorFinanceApi(transport).getCategories("Bearer $token") }
            failed.await()
            request.cancelAndJoin()
        }
        assertEquals(1, calls)
    }

    @Test
    fun loginAndHomeUseSharedTransportAndPreserveValues() = runTest {
        withTransport({ request ->
            val response = when (request.url.encodedPath) {
                "/base/api/auth/login" -> {
                    assertNull(request.headers[HttpHeaders.Authorization])
                    assertEquals(JsonObject(mapOf("email" to JsonPrimitive("teste@example.invalid"), "password" to JsonPrimitive("synthetic-password"))),
                        FinanceJson.parseToJsonElement((request.body as TextContent).text))
                    """{"message":"ok","token":"synthetic-token","user":{"name":"Teste","email":"teste@example.invalid","role":"user"}}"""
                }
                "/base/api/reports/summary" -> summary
                "/base/api/incomes/" -> """{"incomes":[{"id":1,"source":"Salário","amount":2000,"month":10,"year":2026}],"total":1}"""
                "/base/api/expenses" -> {
                    assertEquals("effective", request.url.parameters["period_mode"])
                    """{"expenses":[$expense],"total":1}"""
                }
                "/base/api/categories/" -> """{"categories":[{"id":4,"name":"Mercado"}],"total":1}"""
                else -> error("Endpoint inesperado")
            }
            if (!request.url.encodedPath.endsWith("/login")) {
                assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
            }
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { transport ->
            val auth = SharedAuthRepository(KtorAuthApi(transport))
            assertEquals(token, auth.login("teste@example.invalid", "synthetic-password").token)
            val repository = SharedFinanceRepository(KtorFinanceApi(transport))
            assertEquals(1799.75, repository.getSummary(token, 10, 2026).total_geral_disponivel)
            assertEquals(2000.0, repository.getIncomes(token, 10, 2026).incomes.single().amount)
            assertEquals("Mercado", repository.getCategories(token).categories.single().name)
            val loaded = repository.getEffectiveExpenses(token, 10, 2026, null).expenses.single()
            assertEquals(200.25, loaded.amount)
            assertEquals("", loaded.description)
            assertEquals("", loaded.date)
            assertEquals("", loaded.type)
            assertEquals("", loaded.payment_splits.single().payment_source)
            assertEquals(200.25, loaded.payment_splits.single().amount)
            assertTrue(loaded.isAdvanced)
            assertTrue(loaded.is_paid)
            assertEquals("2026-10-01", loaded.advancedAt)
        }
    }

    private data class Call(val method: String, val path: String, val query: Map<String, String> = emptyMap(),
        val body: String? = null, val invoke: suspend (SharedFinanceApi) -> Any?)

    @Test
    fun everyJsonEndpointPreservesHttpMethodPathQueryAndBody() = runTest {
        val month = mapOf("month" to "10", "year" to "2026")
        val cases = listOf(
            Call("GET", "api/app-version", mapOf("platform" to "ios")) { it.getAppVersion("ios") },
            Call("POST", "api/assistant/chat", body = """{"message":"Olá","conversation_id":7}""") { it.chatAssistant("Bearer $token", AssistantChatRequest("Olá", 7)) },
            Call("GET", "api/assistant/conversations") { it.getAssistantConversations("Bearer $token") },
            Call("GET", "api/assistant/conversations/7/messages") { it.getAssistantMessages("Bearer $token", 7) },
            Call("DELETE", "api/assistant/conversations/7") { it.deleteAssistantConversation("Bearer $token", 7) },
            Call("GET", "api/reports/summary", month) { it.getSummary("Bearer $token", 10, 2026) },
            Call("GET", "api/categories/") { it.getCategories("Bearer $token") },
            Call("GET", "api/expenses", month + mapOf("payment_status" to "paid", "period_mode" to "effective")) { it.getExpenses("Bearer $token", 10, 2026, "paid", "effective") },
            Call("GET", "api/incomes/", month) { it.getIncomes("Bearer $token", 10, 2026) },
            Call("POST", "api/expenses/", body = """{"amount":12.5,"description":"Compra","category_id":4,"payment_source":"Salário","date":"2026-10-01","type":"Única","installments":1,"notes":""}""") { it.createExpense("Bearer $token", ExpenseRequest(12.5, "Compra", 4, "Salário", date = "2026-10-01", type = "Única", installments = 1)) },
            Call("PATCH", "api/expenses/10", body = """{"amount":15.0,"update_future":false}""") { it.updateExpense("Bearer $token", 10, ExpenseUpdateRequest(amount = 15.0, update_future = false)) },
            Call("PATCH", "api/expenses/10/payment-status", body = """{"is_paid":false}""") { it.updateExpensePaymentStatus("Bearer $token", 10, ExpensePaymentStatusRequest(false)) },
            Call("PATCH", "api/expenses/10/advance-status", body = """{"is_advanced":true,"advanced_at":"2026-10-01"}""") { it.updateAdvanceStatus("Bearer $token", 10, UpdateAdvanceStatusRequest(true, "2026-10-01")) },
            Call("DELETE", "api/expenses/10", mapOf("delete_future" to "false")) { it.deleteExpense("Bearer $token", 10, false) },
            Call("GET", "api/expenses/10") { it.getExpenseById("Bearer $token", 10) },
            Call("POST", "api/incomes/", body = """{"source":"Salário","amount":100.0,"month":10,"year":2026,"type":"Única","repeat_future":true}""") { it.createIncome("Bearer $token", IncomeRequest("Salário", 100.0, 10, 2026, "Única", true)) },
            Call("PATCH", "api/incomes/1", body = """{"amount":110.0,"update_future":true}""") { it.updateIncome("Bearer $token", 1, IncomeUpdateRequest(110.0, true)) },
            Call("DELETE", "api/incomes/1", mapOf("delete_future" to "true")) { it.deleteIncome("Bearer $token", 1, true) },
            Call("POST", "api/categories/", body = """{"name":"Mercado"}""") { it.createCategory("Bearer $token", CategoryRequest("Mercado")) },
            Call("PATCH", "api/categories/4", body = """{"name":"Casa"}""") { it.updateCategory("Bearer $token", 4, CategoryRequest("Casa")) },
            Call("DELETE", "api/categories/4") { it.deleteCategory("Bearer $token", 4) },
            Call("PATCH", "api/users/profile", body = """{"name":"Teste","email":"teste@example.invalid"}""") { it.updateProfile("Bearer $token", UpdateProfileRequest("Teste", "teste@example.invalid")) },
            Call("GET", "api/users/profile") { it.getProfile("Bearer $token") },
            Call("DELETE", "api/users/profile/photo") { it.deleteProfilePhoto("Bearer $token") },
            Call("GET", "api/reports/categories", month) { it.getReportCategories("Bearer $token", 10, 2026) },
            Call("GET", "api/reports/chart", mapOf("year" to "2026")) { it.getReportChart("Bearer $token", 2026) },
            Call("GET", "api/reports/yearly-summary", mapOf("year" to "2026")) { it.getYearlySummary("Bearer $token", 2026) },
            Call("GET", "api/reports/month-comparison", month + mapOf("compare_month" to "9", "compare_year" to "2025")) { it.getMonthComparison("Bearer $token", 10, 2026, 9, 2025) },
            Call("GET", "api/reports/installment-commitments", month + mapOf("months" to "12", "include_current_month_as_paid" to "false")) { it.getInstallmentCommitments("Bearer $token", 12, 10, 2026, false) }
        )
        for (case in cases) {
            var called = false
            withTransport({ request ->
                called = true
                assertEquals(case.method, request.method.value)
                assertEquals("/base/${case.path}", request.url.encodedPath)
                assertEquals(case.query, request.url.parameters.entries().associate { it.key to it.value.single() })
                assertEquals(if (case.path == "api/app-version") null else "Bearer $token", request.headers[HttpHeaders.Authorization])
                case.body?.let { assertEquals(FinanceJson.parseToJsonElement(it), FinanceJson.parseToJsonElement((request.body as TextContent).text)) }
                respond("""{"error":"synthetic-error"}""", HttpStatusCode.BadRequest)
            }) { transport ->
                assertFailsWith<ApiResponseException> { case.invoke(KtorFinanceApi(transport)) }
            }
            assertTrue(called, case.path)
        }
    }

    @Test
    fun absentOptionalQueriesAndNullFieldsAreOmittedButFalseIsSent() = runTest {
        withTransport({ request ->
            assertEquals(setOf("month", "year"), request.url.parameters.names())
            respond("""{"expenses":[],"total":0}""")
        }) { transport ->
            KtorFinanceApi(transport).getExpenses("Bearer $token", 10, 2026)
        }
    }

    @Test
    fun financialNullsAndMissingAmountsAreRejectedRatherThanZeroed() {
        for (value in listOf("null", "\"inválido\"")) {
            assertFailsWith<SerializationException> { FinanceJson.decodeFromString<Expense>("""{"id":10,"category_id":4,"amount":$value}""") }
        }
        assertFailsWith<SerializationException> { FinanceJson.decodeFromString<Expense>("""{"id":10,"category_id":4}""") }
    }

    @Test
    fun unknownFieldsAndLegacyMissingPresentationRemainCompatible() {
        val result = FinanceJson.decodeFromString<Expense>("""{"id":10,"category_id":4,"amount":2.5,"future_field":true}""")
        assertEquals("", result.description)
        assertTrue(result.payment_splits.isEmpty())
        assertFalse(result.isAdvanced)
    }

    @Test
    fun unauthorizedLoginDiffersFromExpiredAuthenticatedSession() = runTest {
        withTransport({ respond("""{"message":"Sessão inválida"}""", HttpStatusCode.Unauthorized) }) { transport ->
            assertFailsWith<SessionExpiredException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
            val loginError = assertFailsWith<ApiResponseException> { KtorAuthApi(transport).login(LoginRequest("teste@example.invalid", "synthetic-password")) }
            assertEquals(401, loginError.statusCode)
            assertEquals("Sessão inválida", loginError.apiMessage)
        }
    }

    @Test
    fun revokedAccessNotifiesSharedSessionEvent() = runTest {
        val event = async(start = CoroutineStart.UNDISPATCHED) { SessionAccessEvents.accessRevoked.first() }
        withTransport({ respond("""{"detail":"Acesso revogado pelo administrador"}""", HttpStatusCode.Forbidden) }) { transport ->
            assertFailsWith<ApiResponseException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
        }
        assertEquals("Acesso revogado pelo administrador", event.await())
    }

    @Test
    fun timeoutNetworkAndCancellationKeepTheirMeaning() = runTest {
        withTransport({ request -> throw HttpRequestTimeoutException(request) }) { transport ->
            val error = assertFailsWith<NetworkRequestException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
            assertEquals("O servidor demorou para responder. Tente novamente.", error.userMessage)
        }
        withTransport({ throw IOException("synthetic-offline") }) { transport ->
            val error = assertFailsWith<NetworkRequestException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
            assertEquals("Não foi possível conectar ao servidor. Verifique sua internet.", error.userMessage)
        }
        withTransport({ throw CancellationException("synthetic-cancel") }) { transport ->
            assertFailsWith<CancellationException> { KtorFinanceApi(transport).getCategories("Bearer $token") }
        }
    }

    @Test
    fun emptyPhotoDeletionAndNullableCategoryReportPreserveExistingFallbacks() = runTest {
        withTransport({ request -> if (request.method == HttpMethod.Delete) respond("", HttpStatusCode.NoContent) else respond("null") }) { transport ->
            val repository = SharedFinanceRepository(KtorFinanceApi(transport))
            assertEquals("", repository.deleteProfilePhoto(token).message)
            assertTrue(repository.getReportCategories(token, 10, 2026).isEmpty())
        }
    }

    @Test
    fun configurationRequiresHttpsAndKeepsBasePath() {
        assertEquals("https://example.invalid/base/", ApiConfiguration(" https://example.invalid/base ").baseUrl)
        for (url in listOf("http://example.invalid/", "https://user:pass@example.invalid/", "https://example.invalid/?token=secret", "https://example.invalid/#fragment")) {
            assertFailsWith<IllegalArgumentException> { ApiConfiguration(url) }
        }
    }

    @Test
    fun readRedirectsPreserveSameHostAuthorizationAndStripItAcrossHosts() = runTest {
        for (destination in listOf("https://example.invalid/new-path", "https://other.example.invalid/new-path")) {
            var calls = 0
            withTransport({ request ->
                calls++
                if (calls == 1) respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, destination))
                else {
                    assertEquals(destination, request.url.toString())
                    assertEquals(if (request.url.host == "example.invalid") "Bearer $token" else null,
                        request.headers[HttpHeaders.Authorization])
                    respond("""{"categories":[],"total":0}""")
                }
            }) { transport -> assertTrue(KtorFinanceApi(transport).getCategories("Bearer $token").categories.isEmpty()) }
            assertEquals(2, calls)
        }
    }

    @Test
    fun redirectsDoNotDowngradeHttpsOrReplayFinancialWrites() = runTest {
        var calls = 0
        withTransport({
            calls++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "http://example.invalid/unsafe"))
        }) { transport -> assertFailsWith<ApiResponseException> { KtorFinanceApi(transport).getCategories("Bearer $token") } }
        assertEquals(1, calls)
        calls = 0
        withTransport({
            calls++
            respond("", HttpStatusCode.TemporaryRedirect, headersOf(HttpHeaders.Location, "https://example.invalid/new-path"))
        }) { transport ->
            assertFailsWith<ApiResponseException> {
                KtorFinanceApi(transport).createExpense("Bearer $token", ExpenseRequest(1.0, "Teste", 4,
                    date = "2026-10-01", type = "Única", installments = 1))
            }
        }
        assertEquals(1, calls)
    }
}
