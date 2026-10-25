package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.HomeDataSource
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.despesas.filterExpenses
import com.example.appfinanceiro.feature.despesas.components.PaymentSplitInput
import com.example.appfinanceiro.feature.despesas.components.paymentSplitValidationMessage
import com.example.appfinanceiro.feature.home.calculateIncomeTotals
import com.example.appfinanceiro.feature.home.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BenefitsTest {
    @Test fun benefitLabelsKeepFullNamesAccentsAndUppercaseAbbreviations() {
        assertEquals("Vale-alimentação (VA)", paymentSourceDisplayName("VA"))
        assertEquals("Vale-refeição (VR)", paymentSourceDisplayName("VR"))
        assertEquals("Vale-alimentação (VA)", paymentSourceDisplayName("vale_alimentacao"))
        assertEquals("Vale-refeição (VR)", paymentSourceDisplayName("vale-refeição"))
        assertEquals("Salário", paymentSourceDisplayName("Salario"))
        assertEquals("Renda Extra", paymentSourceDisplayName("renda extra"))
    }
    @Test fun visibleHomeModelRetainsIndependentBenefitTotalsWhenIncomesArrive() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val base = EditingFake()
        val source = object : HomeDataSource by base {
            override suspend fun getSummary(token: String, month: Int, year: Int) = base.getSummary(token, month, year).copy(
                va = 600.0, vr = 400.0, restante_va = 500.0, restante_vr = 350.0,
                total_gasto_va = 100.0, total_gasto_vr = 50.0, total_income = 2000.0, total_geral_disponivel = 1750.0)
            override suspend fun getIncomes(token: String, month: Int?, year: Int?) = IncomesResponse(listOf(
                Income(1, "VA", 600.0, 12, 2026), Income(2, "VR", 400.0, 12, 2026),
                Income(3, "VA", 9000.0, 1, 2027)), 3)
        }
        val model = HomeViewModel(source)
        try {
            model.loadAll("synthetic-token", 12, 2026); runCurrent()
            val summary = assertNotNull(model.uiState.value.summaryData)
            assertEquals(600.0, summary.va); assertEquals(400.0, summary.vr)
            assertEquals(500.0, summary.restante_va); assertEquals(350.0, summary.restante_vr)
            assertEquals(2000.0, summary.total_income); assertEquals(1750.0, summary.total_geral_disponivel)
            assertEquals(2, model.uiState.value.incomesData.size)
            model.loadIncomes("synthetic-token", 1, 2027); runCurrent()
            // January income must not rewrite a still-visible December summary.
            assertEquals(600.0, model.uiState.value.summaryData?.va)
            assertEquals(2026, model.uiState.value.summaryData?.year)
            assertEquals(listOf(3), model.uiState.value.incomesData.map { it.id })
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun successfulIncomeMutationsInvalidateOtherScreensWhileFailedWritesDoNot() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var fail = false
        val client = HttpClient(MockEngine {
            if (fail) respond("""{"message":"Falha sintética"}""", HttpStatusCode.ServiceUnavailable)
            else respond("""{"message":"OK"}""")
        })
        val files = object : FinanceFilesDataSource {
            override suspend fun export(token: String, request: ExportRequest): FinanceFile = error("unused")
            override suspend fun uploadPhoto(token: String, photo: ProfilePhoto): ProfilePhotoResponse = error("unused")
            override suspend fun avatar(token: String, reference: String): ByteArray = error("unused")
        }
        val model = FinanceActionsViewModel(SharedFinanceRepository(KtorFinanceApi(ApiTransport(client,
            ApiConfiguration("https://example.invalid/")))), files)
        try {
            val initial = FinanceDataChanges.revision.value
            model.createIncome("synthetic-token", IncomeRequest("VA", 600.0, 12, 2026, "Fixa"))
            model.updateIncome("synthetic-token", 1, IncomeUpdateRequest(650.0, true))
            model.deleteIncome("synthetic-token", 2, true)
            assertEquals(initial + 3, FinanceDataChanges.revision.value)
            fail = true
            assertFails { model.createIncome("synthetic-token", IncomeRequest("VR", 400.0, 12, 2026, "Fixa")) }
            assertEquals(initial + 3, FinanceDataChanges.revision.value)
        } finally { model.close(); client.close(); Dispatchers.resetMain() }
    }

    @Test fun benefitIncomeCreationUsesSalaryRecurrenceAndSelectedYear() {
        for (source in listOf("VA", "VR")) {
            val december = IncomeDraft(source, Period(12, 2026), amount = "600,50").createRequest()
            assertEquals(source, december.source)
            assertEquals("Fixa", december.type)
            assertEquals(12, december.month)
            assertEquals(2026, december.year)
            assertNull(december.repeat_future)
            val january = IncomeDraft(source, Period(12, 2026).shift(1), amount = "400").createRequest()
            assertEquals(1, january.month)
            assertEquals(2027, january.year)
        }
    }

    @Test fun benefitEditsAndDeletesUseEachRecordAndExplicitFutureFlag() = runTest {
        for ((index, source) in listOf("VA", "VR").withIndex()) {
            val income = Income(10 + index, source, 600.0, 12, 2026)
            for (future in listOf(false, true)) {
                val fake = EditingFake()
                val editor = IncomeEditor(fake, "synthetic-token", backgroundScope,
                    IncomeDraft(source, Period(12, 2026), income, amount = "650", updateFuture = future), {}, {})
                editor.save(confirmedFuture = future); runCurrent()
                assertEquals(income.id, fake.updatedIncome?.first)
                assertEquals(IncomeUpdateRequest(650.0, future), fake.updatedIncome?.second)
                val removal = IncomeEditor(fake, "synthetic-token", backgroundScope,
                    IncomeDraft(source, Period(12, 2026), income, deleteFuture = future), {}, {})
                removal.delete(true); runCurrent()
                assertEquals(income.id to if (future) true else null, fake.deletedIncome)
            }
        }
    }

    @Test fun singleAndSplitBenefitsWorkForEveryExpenseTypeAndLegacySources() {
        for (type in EXPENSE_TYPES) {
            for (source in listOf("VA", "VR")) {
                val draft = ExpenseDraft(amount = "150", description = "Compra", categoryId = 1,
                    source = source, type = type, installments = "3", date = "15/12/2026")
                assertNull(draft.validate())
                assertEquals(source, draft.createRequest().payment_source)
                assertNull(draft.createRequest().payment_splits)
                val split = draft.copy(split = true, splits = listOf(SplitDraft(source, "100"), SplitDraft("Salário", "50")))
                val request = split.createRequest()
                assertNull(request.payment_source)
                assertEquals(150.0, request.payment_splits!!.sumOf { it.amount })
                val update = split.updateRequest(testExpense(type = type))
                assertNull(update.payment_source)
                assertEquals(listOf(source, "Salário"), update.payment_splits!!.map { it.payment_source })
            }
        }
        assertNotNull(paymentSplitValidationMessage(150.0, listOf(PaymentSplitInput("VA", "100"), PaymentSplitInput("vale-alimentação", "50"))))
        assertNotNull(paymentSplitValidationMessage(150.0, listOf(PaymentSplitInput("VA", "99"), PaymentSplitInput("VR", "50"))))
        assertNull(paymentSplitValidationMessage(150.0, listOf(PaymentSplitInput("VA", "100"), PaymentSplitInput("VR", "50"))))
    }

    @Test fun filtersIncludeSplitSourcesWithoutMixingVaAndVrOrDroppingOtherCriteria() {
        val va = testExpense(id = 1).copy(payment_source = "VA", is_paid = true)
        val vr = testExpense(id = 2).copy(payment_source = "VR")
        val divided = testExpense(id = 3).copy(payment_source = null,
            payment_splits = listOf(PaymentSplit(payment_source = "VA", amount = 60.0), PaymentSplit(payment_source = "VR", amount = 40.0)))
        val expenses = listOf(va, vr, divided)
        assertEquals(listOf(1, 3), filterExpenses(expenses, "Conta", "Fixas", 1, "VA").map { it.id })
        assertEquals(listOf(2, 3), ExpenseFilters(source = "VR").apply(expenses).map { it.id })
        assertEquals(listOf(1), filterExpenses(expenses, "Conta", "Fixas", 1, "VA", "paid").map { it.id })
        assertTrue(filterExpenses(expenses, "não existe", "Todas", paymentSource = "VA").isEmpty())
    }

    @Test fun monthlySummaryPreservesServerBalancesAndTotalsWithoutCarryOverOrDoubleCounting() = runTest {
        val base = EditingFake()
        val credits = listOf(Income(10, "VA", 600.0, 12, 2026), Income(11, "VR", 400.0, 12, 2026),
            Income(12, "VA", 650.0, 1, 2027))
        val source = object : HomeDataSource by base {
            override suspend fun getIncomes(token: String, month: Int?, year: Int?) = IncomesResponse(credits, credits.size)
            override suspend fun getSummary(token: String, month: Int, year: Int): SummaryResponse =
                base.getSummary(token, month, year).copy(total_income = if (month == 12) 1000.0 else 650.0,
                    total_expense = 150.0, total_geral_disponivel = if (month == 12) 850.0 else 500.0,
                    va = if (month == 12) 600.0 else 650.0, vr = if (month == 12) 400.0 else 0.0,
                    total_gasto_va = 100.0, total_gasto_vr = 50.0,
                    restante_va = if (month == 12) 500.0 else 550.0, restante_vr = if (month == 12) 350.0 else -50.0)
        }
        val presenter = HomePresenter(source, "synthetic-token", backgroundScope, Period(12, 2026)) {}
        presenter.refresh(); runCurrent()
        val december = assertNotNull(presenter.state.value.snapshot).summary
        assertEquals(600.0, december.va); assertEquals(400.0, december.vr)
        assertEquals(1000.0, december.total_income); assertEquals(850.0, december.total_geral_disponivel)
        presenter.selectPeriod(Period(1, 2027)); runCurrent()
        val january = assertNotNull(presenter.state.value.snapshot).summary
        assertEquals(650.0, january.va); assertEquals(0.0, january.vr)
        assertEquals(550.0, january.restante_va); assertEquals(-50.0, january.restante_vr)
        assertEquals(650.0, january.total_income)
        assertNull(credits.incomeFor("VR", 1, 2027))
        assertEquals(12, credits.incomeFor("VA", 1, 2027)?.id)
        assertEquals(600.0, calculateIncomeTotals(credits.filter { it.month == 12 }).va)
    }

    @Test fun failedBenefitWriteKeepsDraftAndDoesNotReportSuccess() = runTest {
        val fake = EditingFake().apply { writeFailure = IllegalStateException("Falha de conexão sintética") }
        var saved = 0
        val editor = IncomeEditor(fake, "synthetic-token", backgroundScope,
            IncomeDraft("VR", Period(1, 2027), amount = "400"), {}, { saved++ })
        editor.save(); runCurrent()
        assertEquals(0, saved); assertNotNull(editor.mutation.state.value.error)
        assertEquals("400", editor.state.value.amount)
        fake.writeFailure = null; editor.save(); runCurrent()
        assertEquals(1, saved); assertEquals("VR", fake.createdIncome?.source)
    }
}
