package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.network.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

private data class CapturedEdit(val method: String, val path: String, val query: Parameters, val body: JsonObject?)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FinanceEditingHttpTest {
    @Test fun editorsUseTheRealRepositoryTransportAndJsonContracts() = runTest {
        val captured = mutableListOf<CapturedEdit>()
        val original = testExpense(type = "Parcelada")
        val client = HttpClient(MockEngine) { engine {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
            assertEquals("Bearer synthetic-token", request.headers[HttpHeaders.Authorization])
            val body = (request.body as? TextContent)?.text?.let { Json.parseToJsonElement(it).jsonObject }
            captured += CapturedEdit(request.method.value, request.url.encodedPath, request.url.parameters, body)
            val content = when {
                request.method == HttpMethod.Get && request.url.encodedPath == "/api/categories/" ->
                    FinanceJson.encodeToString(CategoriesResponse(listOf(Category(1, name = "Casa")), 1))
                request.method == HttpMethod.Get && request.url.encodedPath == "/api/expenses/1" -> FinanceJson.encodeToString(original)
                request.method == HttpMethod.Get && request.url.encodedPath == "/api/reports/summary" -> FinanceJson.encodeToString(
                    SummaryResponse(10, 2026, 1000.0, 0.0, 0.0, 900.0, 0.0, 0.0, 100.0, 0.0, 0.0, 100.0, 900.0, 1000.0))
                request.method == HttpMethod.Get && request.url.encodedPath == "/api/incomes/" -> FinanceJson.encodeToString(IncomesResponse(emptyList(), 0))
                request.method == HttpMethod.Get && request.url.encodedPath == "/api/expenses" -> FinanceJson.encodeToString(ExpensesResponse(listOf(original), 1))
                request.url.encodedPath.endsWith("payment-status") -> FinanceJson.encodeToString(ExpensePaymentStatusResponse("ok", original.copy(is_paid = true)))
                request.url.encodedPath.endsWith("advance-status") -> FinanceJson.encodeToString(AdvanceStatusResponse("ok", original.copy(isAdvanced = true)))
                else -> FinanceJson.encodeToString(DefaultResponse("ok"))
            }
            respond(content, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        } } }
        try {
            val repository = SharedFinanceRepository(KtorFinanceApi(ApiTransport(client, ApiConfiguration("https://example.invalid/"))))
            val income = IncomeEditor(repository, "synthetic-token", backgroundScope,
                IncomeDraft("Renda Extra", Period(10, 2026), amount = "250,50", repeatFuture = true), {}, {})
            income.save(true); runCurrent()
            assertTrue(income.mutation.state.value.completed, income.mutation.state.value.toString())
            val incomeBody = captured.single { it.method == "POST" && it.path == "/api/incomes/" }.body!!
            assertEquals(250.50, incomeBody.getValue("amount").jsonPrimitive.double)
            assertEquals(true, incomeBody.getValue("repeat_future").jsonPrimitive.boolean)
            assertEquals("Fixa", incomeBody.getValue("type").jsonPrimitive.content)

            val editor = ExpenseEditor(repository, "synthetic-token", backgroundScope, 1, {}, {})
            editor.load(); runCurrent()
            assertFalse(editor.state.value.loading, editor.state.value.toString())
            assertNull(editor.state.value.loadError)
            editor.update { it.copy(amount = "150", split = true,
                splits = listOf(SplitDraft("Salário", "100"), SplitDraft("Renda Extra", "50")), updateFuture = false) }
            editor.save(); runCurrent()
            assertTrue(editor.mutation.state.value.completed, editor.mutation.state.value.toString())
            val expenseBody = captured.single { it.method == "PATCH" && it.path == "/api/expenses/1" }.body!!
            assertEquals(false, expenseBody.getValue("update_future").jsonPrimitive.boolean)
            assertEquals(150.0, expenseBody.getValue("amount").jsonPrimitive.double)
            assertEquals(2, expenseBody.getValue("payment_splits").jsonArray.size)
            assertFalse("type" in expenseBody)
            assertFalse("installments" in expenseBody)
            assertFalse("is_paid" in expenseBody)
            assertFalse("is_advanced" in expenseBody)

            val home = HomePresenter(repository, "synthetic-token", backgroundScope, Period(10, 2026)) {}
            val workspace = FinanceWorkspace(home, repository, repository, "synthetic-token", backgroundScope) {}
            workspace.advance(original, FinancialDate(2026, 9, 30), workspace.newAction()); runCurrent()
            workspace.setPaid(original, true, workspace.newAction()); runCurrent()
            workspace.deleteExpense(original, true, workspace.newAction()); runCurrent()
            assertEquals("2026-09-30", captured.single { it.path.endsWith("advance-status") }.body!!.getValue("advanced_at").jsonPrimitive.content)
            assertEquals(true, captured.single { it.path.endsWith("payment-status") }.body!!.getValue("is_paid").jsonPrimitive.boolean)
            assertEquals("true", captured.single { it.method == "DELETE" }.query["delete_future"])
            assertTrue(captured.any { it.method == "GET" && it.path == "/api/expenses" && it.query["period_mode"] == "effective" })
            workspace.close()
        } finally { client.close() }
    }
}
