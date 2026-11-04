package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BalancePreferencesHttpTest {
    private val token = "synthetic-token"
    private val preferencesPath = "/base/api/users/balance-preferences"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val summary = """{"month":11,"year":2026,"salario":1000.0,"adiantamento":0.0,"renda_extra_amt":0.0,"restante_salario":800.0,"restante_adiantamento":0.0,"restante_renda_extra":0.0,"total_gasto_salario":200.0,"total_gasto_adiantamento":0.0,"total_gasto_renda_extra":0.0,"total_expense":900.0,"total_geral_disponivel":1125.0,"total_income":2025.0,"va":725.0,"vr":300.0,"total_gasto_va":600.0,"total_gasto_vr":100.0,"restante_va":125.0,"restante_vr":200.0}"""

    private suspend fun withRepository(
        handler: MockRequestHandler,
        block: suspend (SharedFinanceRepository) -> Unit
    ) {
        val client = HttpClient(MockEngine(handler)) { configureFinanceHttpClient() }
        try {
            block(SharedFinanceRepository(KtorFinanceApi(ApiTransport(client,
                ApiConfiguration("https://example.invalid/base/")))))
        } finally {
            client.close()
        }
    }

    @Test
    fun authenticatedPartialWritesPreserveTheOtherPreferenceAcrossAllFourCombinations() = runTest {
        var saved = BalancePreferences(false, false)
        val writes = mutableListOf<JsonObject>()
        withRepository({ request ->
            assertEquals(preferencesPath, request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
            when (request.method) {
                HttpMethod.Get -> assertNull(request.body as? TextContent)
                HttpMethod.Patch -> {
                    assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
                    val body = (request.body as TextContent).text
                    val patch = FinanceJson.decodeFromString<BalancePreferencesUpdate>(body)
                    writes += FinanceJson.parseToJsonElement(body).jsonObject
                    saved = BalancePreferences(
                        patch.exclude_va_from_available ?: saved.exclude_va_from_available,
                        patch.exclude_vr_from_available ?: saved.exclude_vr_from_available
                    )
                }
                else -> error("Método inesperado")
            }
            respond(FinanceJson.encodeToString(saved), headers = jsonHeaders)
        }) { repository ->
            assertEquals(BalancePreferences(false, false), repository.getBalancePreferences(token))
            val changes = listOf(
                BalancePreferencesUpdate(exclude_va_from_available = true) to BalancePreferences(true, false),
                BalancePreferencesUpdate(exclude_vr_from_available = true) to BalancePreferences(true, true),
                BalancePreferencesUpdate(exclude_va_from_available = false) to BalancePreferences(false, true),
                BalancePreferencesUpdate(exclude_vr_from_available = false) to BalancePreferences(false, false)
            )
            for ((patch, expected) in changes) {
                assertEquals(expected, repository.updateBalancePreferences(token, patch))
                assertEquals(expected, repository.getBalancePreferences(token))
            }
        }
        assertEquals(listOf(
            """{"exclude_va_from_available":true}""",
            """{"exclude_vr_from_available":true}""",
            """{"exclude_va_from_available":false}""",
            """{"exclude_vr_from_available":false}"""
        ).map { FinanceJson.parseToJsonElement(it).jsonObject }, writes)
        assertEquals("{}", FinanceJson.encodeToString(BalancePreferencesUpdate()))
    }

    @Test
    fun missingNullAndInvalidPreferenceFieldsFailWithoutBecomingFalseOrWritingDefaults() = runTest {
        val invalidResponses = listOf(
            "{}",
            """{"exclude_va_from_available":true}""",
            """{"exclude_vr_from_available":false}""",
            """{"exclude_va_from_available":null,"exclude_vr_from_available":false}""",
            """{"exclude_va_from_available":false,"exclude_vr_from_available":null}""",
            """{"exclude_va_from_available":1,"exclude_vr_from_available":false}"""
        )
        for (payload in invalidResponses) {
            var calls = 0
            withRepository({ request ->
                calls++
                assertEquals(HttpMethod.Get, request.method)
                respond(payload, headers = jsonHeaders)
            }) { repository ->
                assertFailsWith<SerializationException> { repository.getBalancePreferences(token) }
            }
            assertEquals(1, calls)
        }
    }

    @Test
    fun authenticationAndServerFailuresPropagateInsteadOfReturningDisabledPreferences() = runTest {
        for (method in listOf(HttpMethod.Get, HttpMethod.Patch)) {
            var calls = 0
            withRepository({ request ->
                calls++
                assertEquals(method, request.method)
                assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
                respond("{}", HttpStatusCode.Unauthorized, jsonHeaders)
            }) { repository ->
                assertFailsWith<SessionExpiredException> {
                    if (method == HttpMethod.Get) repository.getBalancePreferences(token)
                    else repository.updateBalancePreferences(token, BalancePreferencesUpdate(exclude_va_from_available = true))
                }
            }
            assertEquals(1, calls)
        }
        var calls = 0
        withRepository({ request ->
            calls++
            assertEquals(HttpMethod.Get, request.method)
            respond("""{"error":"Falha sintética"}""", HttpStatusCode.InternalServerError, jsonHeaders)
        }) { repository ->
            assertEquals(500, assertFailsWith<ApiResponseException> { repository.getBalancePreferences(token) }.statusCode)
        }
        assertEquals(1, calls)
    }

    @Test
    fun lostPatchResponsesAreNeverResubmittedAndCancellationIsPreserved() = runTest {
        var calls = 0
        withRepository({ request ->
            calls++
            assertEquals(HttpMethod.Patch, request.method)
            throw IOException("synthetic-response-lost")
        }) { repository ->
            assertFailsWith<NetworkRequestException> {
                repository.updateBalancePreferences(token, BalancePreferencesUpdate(exclude_vr_from_available = false))
            }
        }
        assertEquals(1, calls)
        withRepository({ throw CancellationException("synthetic-cancel") }) { repository ->
            assertFailsWith<CancellationException> {
                repository.updateBalancePreferences(token, BalancePreferencesUpdate(exclude_va_from_available = true))
            }
        }
    }

    @Test
    fun summaryUsesTheServerAvailableTotalAndPreservesEverySourceForAllCombinations() = runTest {
        val cases = listOf(
            BalancePreferences(false, false) to 1125.0,
            BalancePreferences(true, false) to 1000.0,
            BalancePreferences(false, true) to 925.0,
            BalancePreferences(true, true) to 800.0
        )
        val original = FinanceJson.decodeFromString<SummaryResponse>(summary)
        for ((preferences, available) in cases) {
            val payload = FinanceJson.parseToJsonElement(summary).jsonObject.toMutableMap().apply {
                put("exclude_va_from_available", JsonPrimitive(preferences.exclude_va_from_available))
                put("exclude_vr_from_available", JsonPrimitive(preferences.exclude_vr_from_available))
                put("total_geral_disponivel", JsonPrimitive(available))
            }
            withRepository({ request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/base/api/reports/summary", request.url.encodedPath)
                assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
                assertEquals(setOf("month", "year"), request.url.parameters.names())
                assertEquals("11", request.url.parameters["month"])
                assertEquals("2026", request.url.parameters["year"])
                respond(JsonObject(payload).toString(), headers = jsonHeaders)
            }) { repository ->
                assertEquals(original.copy(
                    total_geral_disponivel = available,
                    exclude_va_from_available = preferences.exclude_va_from_available,
                    exclude_vr_from_available = preferences.exclude_vr_from_available
                ), repository.getSummary(token, 11, 2026))
            }
        }
    }

    @Test
    fun legacySummaryDefaultsAndFuturePreferenceFieldsRemainCompatible() {
        val legacy = FinanceJson.decodeFromString<SummaryResponse>(summary)
        assertFalse(legacy.exclude_va_from_available)
        assertFalse(legacy.exclude_vr_from_available)
        assertEquals(1125.0, legacy.total_geral_disponivel)
        val preferences = FinanceJson.decodeFromString<BalancePreferences>(
            """{"exclude_va_from_available":true,"exclude_vr_from_available":false,"future_field":1}"""
        )
        assertEquals(BalancePreferences(true, false), preferences)
    }
}
