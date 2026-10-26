package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

class PatrimonyCategoriesHttpTest {
    private val timestamp = "2026-10-26T12:00:00Z"
    private suspend fun withRepository(handler: MockRequestHandler, block: suspend (SharedPatrimonyRepository) -> Unit) {
        val client = HttpClient(MockEngine(handler)) { configureFinanceHttpClient() }
        try { block(SharedPatrimonyRepository(ApiTransport(client, ApiConfiguration("https://example.invalid/base/")))) }
        finally { client.close() }
    }

    @Test fun categoryLifecycleAndIdAssociationUseTheirOwnCatalogAndPreserveBalanceDates() = runTest {
        val standard = InvestmentCategory(1, "CDB", true)
        var personal: InvestmentCategory? = null
        var investment: Investment? = null
        val paths = mutableListOf<String>()
        withRepository({ request ->
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            val path = request.url.encodedPath
            paths += path
            val body = (request.body as? TextContent)?.text
            val payload = when (request.method to path) {
                HttpMethod.Get to "/base/api/patrimony/investment-categories" -> {
                    assertNull(body)
                    val categories = listOfNotNull(standard, personal)
                    FinanceJson.encodeToString(InvestmentCategoriesResponse(categories.size, categories))
                }
                HttpMethod.Post to "/base/api/patrimony/investment-categories",
                HttpMethod.Patch to "/base/api/patrimony/investment-categories/25" -> {
                    val json = FinanceJson.parseToJsonElement(assertNotNull(body)).jsonObject
                    assertEquals(setOf("name"), json.keys)
                    val name = FinanceJson.decodeFromString<InvestmentCategoryWriteRequest>(body).name
                    assertEquals(name.trim(), name)
                    personal = InvestmentCategory(25, name, false)
                    investment = investment?.let { if (it.category_id == 25) it.copy(category = name) else it }
                    FinanceJson.encodeToString(InvestmentCategoryWriteResponse(personal!!))
                }
                HttpMethod.Post to "/base/api/patrimony/investments",
                HttpMethod.Patch to "/base/api/patrimony/investments/7" -> {
                    val json = FinanceJson.parseToJsonElement(assertNotNull(body)).jsonObject
                    assertEquals(setOf("name", "category_id", "amount"), json.keys)
                    assertFalse(json.getValue("amount").jsonPrimitive.isString)
                    val input = FinanceJson.decodeFromString<InvestmentWriteRequest>(body)
                    assertNull(input.category)
                    val category = if (input.category_id == 1) standard else assertNotNull(personal)
                    assertEquals(category.id, input.category_id)
                    investment = Investment(7, input.name, category.name, input.amount, timestamp, timestamp, category.id)
                    FinanceJson.encodeToString(InvestmentWriteResponse(investment!!))
                }
                HttpMethod.Get to "/base/api/patrimony/" -> {
                    val amount = investment?.amount ?: PatrimonyAmount.Zero
                    FinanceJson.encodeToString(PatrimonyResponse(null, listOfNotNull(investment), PatrimonyAmount.Zero, amount, amount))
                }
                HttpMethod.Delete to "/base/api/patrimony/investment-categories/25" -> {
                    assertNull(body)
                    assertTrue(investment?.category_id != 25)
                    personal = null
                    """{"message":"Categoria excluída"}"""
                }
                else -> error("Rota inesperada")
            }
            respond(payload, if (request.method == HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) { repository ->
            val catalog: InvestmentCategoriesDataSource = repository
            assertEquals(listOf(standard), catalog.getCategories("synthetic-token").categories)
            val created = catalog.createCategory("synthetic-token", "  Minha categoria  ")
            assertEquals(InvestmentCategory(25, "Minha categoria", false), created)
            assertEquals(2, catalog.getCategories("synthetic-token").total)
            val createdInvestment = repository.createInvestment("synthetic-token", investmentRequest("Reserva", created.id, "0,01"))
            assertEquals(25, createdInvestment.category_id)
            val renamed = catalog.renameCategory("synthetic-token", 25, "  Minha categoria renomeada  ")
            assertEquals("Minha categoria renomeada", renamed.name)
            val refreshed = repository.getPatrimony("synthetic-token").investments.single()
            assertEquals(renamed.name, refreshed.category)
            assertEquals(25, refreshed.category_id)
            assertEquals(createdInvestment.amount, refreshed.amount)
            assertEquals(timestamp, refreshed.updated_at)
            repository.updateInvestment("synthetic-token", 7, investmentRequest("Reserva", standard.id, "0,01"))
            catalog.deleteCategory("synthetic-token", 25)
            assertEquals(listOf(standard), catalog.getCategories("synthetic-token").categories)
            assertEquals(standard.id, repository.getPatrimony("synthetic-token").investments.single().category_id)
        }
        assertTrue(paths.all { it.startsWith("/base/api/patrimony/") })
    }

    @Test fun catalogAuthenticationProtectionConflictsAndAbsenceKeepTheirStatusAndMessage() = runTest {
        var unauthorizedCalls = 0
        withRepository({ unauthorizedCalls++; respond("{}", HttpStatusCode.Unauthorized) }) { repository ->
            assertFailsWith<SessionExpiredException> { repository.getCategories("synthetic-token") }
        }
        assertEquals(1, unauthorizedCalls)
        data class Failure(val status: HttpStatusCode, val code: String, val message: String,
            val action: suspend (SharedPatrimonyRepository) -> Unit)
        val failures = listOf(
            Failure(HttpStatusCode.Forbidden, "default_category", "Categoria padrão protegida") { it.renameCategory("synthetic-token", 1, "Outro nome") },
            Failure(HttpStatusCode.Forbidden, "default_category", "Categoria padrão protegida") { it.deleteCategory("synthetic-token", 1) },
            Failure(HttpStatusCode.Conflict, "category_exists", "Categoria já existente") { it.createCategory("synthetic-token", "CDB") },
            Failure(HttpStatusCode.Conflict, "category_in_use", "Categoria em uso") { it.deleteCategory("synthetic-token", 25) },
            Failure(HttpStatusCode.NotFound, "", "Catálogo indisponível") { it.getCategories("synthetic-token") },
            Failure(HttpStatusCode.NotFound, "", "Categoria não encontrada") { it.renameCategory("synthetic-token", 25, "Outro nome") }
        )
        for (failure in failures) {
            var calls = 0
            withRepository({
                calls++
                respond(buildJsonObject { put("error", failure.message); put("code", failure.code) }.toString(), failure.status)
            }) { repository ->
                val error = assertFailsWith<ApiResponseException> { failure.action(repository) }
                assertEquals(failure.status.value, error.statusCode)
                assertEquals(failure.message, error.apiMessage)
            }
            assertEquals(1, calls)
        }
    }

    @Test fun invalidNamesIdsAndAmbiguousInvestmentCategoriesNeverReachTheNetwork() = runTest {
        var calls = 0
        withRepository({ calls++; error("Entrada inválida não deve fazer requisição") }) { repository ->
            for (name in listOf("", " ", "x".repeat(81))) {
                assertFailsWith<IllegalArgumentException> { repository.createCategory("synthetic-token", name) }
                assertFailsWith<IllegalArgumentException> { repository.renameCategory("synthetic-token", 25, name) }
            }
            for (id in listOf(0, -1)) {
                assertFailsWith<IllegalArgumentException> { repository.renameCategory("synthetic-token", id, "Teste") }
                assertFailsWith<IllegalArgumentException> { repository.deleteCategory("synthetic-token", id) }
                assertFailsWith<IllegalArgumentException> { repository.createInvestment("synthetic-token",
                    InvestmentWriteRequest("Reserva", amount = PatrimonyAmount.Zero, category_id = id)) }
            }
            val missing = InvestmentWriteRequest("Reserva", amount = PatrimonyAmount.Zero)
            val ambiguous = InvestmentWriteRequest("Reserva", "CDB", PatrimonyAmount.Zero, category_id = 25)
            for (request in listOf(missing, ambiguous)) {
                assertFailsWith<IllegalArgumentException> { repository.createInvestment("synthetic-token", request) }
                assertFailsWith<IllegalArgumentException> { repository.updateInvestment("synthetic-token", 7, request) }
            }
        }
        assertEquals(0, calls)
    }
}
