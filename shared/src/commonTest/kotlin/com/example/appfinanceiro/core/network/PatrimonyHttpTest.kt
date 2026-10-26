package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

class PatrimonyHttpTest {
    private val timestamp = "2026-10-26T12:00:00Z"
    private suspend fun withRepository(handler: MockRequestHandler, block: suspend (SharedPatrimonyRepository) -> Unit) {
        val client = HttpClient(MockEngine(handler)) { configureFinanceHttpClient() }
        try { block(SharedPatrimonyRepository(ApiTransport(client, ApiConfiguration("https://example.invalid/base/")))) }
        finally { client.close() }
    }

    @Test fun fullLifecycleUsesDedicatedRoutesAndAuthoritativeSnapshotWithoutTouchingMonthlyFinances() = runTest {
        var fgts: FgtsBalance? = null
        var investment: Investment? = null
        val paths = mutableListOf<String>()
        withRepository({ request ->
            val path = request.url.encodedPath
            paths += path
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            val body = (request.body as? TextContent)?.text
            val response = when (request.method to path) {
                HttpMethod.Get to "/base/api/patrimony/" -> {
                    val a = fgts?.amount ?: PatrimonyAmount.Zero
                    val b = investment?.amount ?: PatrimonyAmount.Zero
                    FinanceJson.encodeToString(PatrimonyResponse(fgts, listOfNotNull(investment), a, b, PatrimonyAmount(a.cents + b.cents)))
                }
                HttpMethod.Put to "/base/api/patrimony/fgts" -> {
                    val json = FinanceJson.parseToJsonElement(assertNotNull(body)).jsonObject
                    assertEquals(setOf("amount"), json.keys)
                    assertFalse(json.getValue("amount").jsonPrimitive.isString)
                    val amount = FinanceJson.decodeFromString<FgtsWriteRequest>(body).amount
                    fgts = FgtsBalance(amount, timestamp)
                    FinanceJson.encodeToString(FgtsWriteResponse(fgts!!))
                }
                HttpMethod.Delete to "/base/api/patrimony/fgts" -> {
                    assertNull(body); fgts = null; "{\"message\":\"Removido\"}"
                }
                HttpMethod.Post to "/base/api/patrimony/investments", HttpMethod.Patch to "/base/api/patrimony/investments/1" -> {
                    val json = FinanceJson.parseToJsonElement(assertNotNull(body)).jsonObject
                    assertEquals(setOf("name", "category", "amount"), json.keys)
                    assertFalse(json.getValue("amount").jsonPrimitive.isString)
                    assertFalse('e' in json.getValue("amount").jsonPrimitive.content.lowercase())
                    val input = FinanceJson.decodeFromString<InvestmentWriteRequest>(body)
                    investment = Investment(1, input.name, requireNotNull(input.category), input.amount, timestamp, timestamp)
                    FinanceJson.encodeToString(InvestmentWriteResponse(investment!!))
                }
                HttpMethod.Delete to "/base/api/patrimony/investments/1" -> {
                    assertNull(body); investment = null; "{\"message\":\"Excluído\"}"
                }
                else -> error("Unexpected route")
            }
            respond(response, if (request.method == HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) { repository ->
            val empty = repository.getPatrimony("synthetic-token")
            assertNull(empty.fgts); assertTrue(empty.investments.isEmpty())
            assertEquals(PatrimonyAmount.Zero, empty.total_patrimony)
            repository.updateFgts("synthetic-token", PatrimonyAmount.Zero)
            assertEquals(PatrimonyAmount.Zero, repository.getPatrimony("synthetic-token").fgts?.amount)
            repository.updateFgts("synthetic-token", PatrimonyAmount(250050))
            assertEquals(PatrimonyAmount(250050), repository.getPatrimony("synthetic-token").total_fgts)
            repository.createInvestment("synthetic-token", investmentRequest("Reserva", "Categoria livre", "9.999.999.999,99"))
            assertEquals(PatrimonyAmount(1000000250049), repository.getPatrimony("synthetic-token").total_patrimony)
            repository.updateInvestment("synthetic-token", 1, investmentRequest("Reserva editada", "Tesouro", "0,01"))
            val reloaded = repository.getPatrimony("synthetic-token")
            assertEquals("Reserva editada", reloaded.investments.single().name)
            assertEquals(PatrimonyAmount(250051), reloaded.total_patrimony)
            repository.deleteInvestment("synthetic-token", 1)
            repository.deleteFgts("synthetic-token")
            assertNull(repository.getPatrimony("synthetic-token").fgts)
        }
        assertTrue(paths.all { it.startsWith("/base/api/patrimony/") })
    }

    @Test fun authenticationAndAccessErrorsKeepTheExistingTransportSemantics() = runTest {
        withRepository({ respond("{}", HttpStatusCode.Unauthorized) }) { repository ->
            assertFailsWith<SessionExpiredException> { repository.getPatrimony("synthetic-token") }
        }
        withRepository({ respond("{\"error\":\"Acesso bloqueado\"}", HttpStatusCode.Forbidden) }) { repository ->
            val failure = assertFailsWith<ApiResponseException> { repository.getPatrimony("synthetic-token") }
            assertEquals(403, failure.statusCode)
            assertEquals("Acesso bloqueado", failure.apiMessage)
        }
        withRepository({ respond("{\"error\":\"Não encontrado\"}", HttpStatusCode.NotFound) }) { repository ->
            assertEquals(404, assertFailsWith<ApiResponseException> { repository.deleteInvestment("synthetic-token", 10) }.statusCode)
        }
    }

    @Test fun returningToPatrimonyRecoversFromTransientConnectionFailures() = runTest {
        var calls = 0
        withRepository({ request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/base/api/patrimony/", request.url.encodedPath)
            calls++
            if (calls < 3) throw IOException("synthetic-connection-reset")
            respond(FinanceJson.encodeToString(PatrimonyResponse(null, emptyList(),
                PatrimonyAmount.Zero, PatrimonyAmount.Zero, PatrimonyAmount.Zero)),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) { repository ->
            val data = repository.getPatrimony("synthetic-token")
            assertEquals(PatrimonyAmount.Zero, data.total_patrimony)
        }
        assertEquals(3, calls)
    }

    @Test fun aLostWriteResponseNeverResubmitsTheFinancialMutation() = runTest {
        var calls = 0
        withRepository({ calls++; throw IOException("synthetic-response-lost") }) { repository ->
            assertFailsWith<NetworkRequestException> { repository.updateFgts("synthetic-token", PatrimonyAmount(100)) }
        }
        assertEquals(1, calls)
    }
}
