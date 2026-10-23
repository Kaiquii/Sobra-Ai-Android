package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.home.HomeViewModel
import com.example.appfinanceiro.feature.despesas.DespesasViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.io.IOException
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialLoadingRecoveryTest {
    private fun offline() = NetworkRequestException("Falha sintética de conexão", IOException("synthetic"))

    @Test fun homeExpensesDoNotWaitForCategoriesAndRemainVisibleIfCategoriesFail() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val fake = EditingFake()
        var fail = true
        val source = object : HomeDataSource by fake {
            override suspend fun getCategories(token: String): CategoriesResponse {
                gate.await()
                if (fail) throw offline()
                return fake.getCategories(token)
            }
        }
        val model = HomeViewModel(source)
        try {
            model.loadAll("synthetic", 10, 2026); runCurrent()
            assertEquals(listOf(fake.expense), model.uiState.value.expensesData)
            assertNotNull(model.uiState.value.summaryData)
            assertTrue(model.uiState.value.isCategoriesLoading)
            gate.complete(Unit); runCurrent()
            assertNull(model.uiState.value.expensesError)
            assertNotNull(model.uiState.value.categoriesError)
            assertEquals(listOf(fake.expense), model.uiState.value.expensesData)
            fail = false
            model.loadCategories("synthetic"); runCurrent()
            assertNull(model.uiState.value.categoriesError)
            assertEquals("Casa", model.uiState.value.categoriesMap[1])
            assertEquals(1, fake.effectiveReads)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun scheduledAndEffectiveExpensesAreNotBlockedByCategoriesFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = EditingFake()
        val source = object : ExpensesDataSource by fake {
            override suspend fun getCategories(token: String): CategoriesResponse = throw offline()
        }
        val model = DespesasViewModel(source)
        try {
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertEquals(fake.scheduled, model.uiState.value.expensesData)
            assertEquals(fake.effective, model.uiState.value.effectiveExpensesData)
            assertNull(model.uiState.value.errorMessage)
            assertNotNull(model.uiState.value.categoriesError)
            assertFalse(model.uiState.value.isLoading)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun repeatedHomeLoadsReuseOnlyIdenticalReadsStillInFlight() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val fake = EditingFake().apply { beforeRead = { gate.await() } }
        var categories = 0
        var incomes = 0
        val source = object : HomeDataSource by fake {
            override suspend fun getCategories(token: String): CategoriesResponse {
                categories++; gate.await(); return fake.getCategories(token)
            }
            override suspend fun getIncomes(token: String, month: Int?, year: Int?): IncomesResponse {
                incomes++; gate.await(); return fake.getIncomes(token, month, year)
            }
        }
        val model = HomeViewModel(source)
        try {
            repeat(3) { model.loadAll("synthetic", 10, 2026); runCurrent() }
            assertEquals(1, fake.summaryReads)
            assertEquals(1, fake.effectiveReads)
            assertEquals(1, categories)
            assertEquals(1, incomes)
            gate.complete(Unit); runCurrent()
            assertFalse(model.uiState.value.isExpensesLoading)
            model.loadAll("synthetic", 10, 2026); runCurrent()
            assertEquals(2, fake.summaryReads)
            assertEquals(2, fake.effectiveReads)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun repeatedExpenseLoadsDoNotCancelAndRestartAnIdenticalPendingRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val fake = EditingFake().apply { beforeRead = { gate.await() } }
        var categories = 0
        val source = object : ExpensesDataSource by fake {
            override suspend fun getCategories(token: String): CategoriesResponse {
                categories++; gate.await(); return fake.getCategories(token)
            }
        }
        val model = DespesasViewModel(source)
        try {
            repeat(3) { model.loadExpenses("synthetic", 10, 2026); runCurrent() }
            assertEquals(1, fake.scheduledReads)
            assertEquals(1, categories)
            gate.complete(Unit); runCurrent()
            assertEquals(1, fake.effectiveReads)
            assertEquals(fake.scheduled, model.uiState.value.expensesData)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun savedChangesStartANewReadEvenWhenTheSamePeriodIsAlreadyLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        val fake = EditingFake()
        var calls = 0
        val source = object : HomeDataSource by fake {
            override suspend fun getEffectiveExpenses(token: String, month: Int, year: Int, paymentStatus: String?): ExpensesResponse {
                val id = ++calls
                withContext(NonCancellable) { if (id == 1) first.await() else second.await() }
                return ExpensesResponse(listOf(testExpense(id)), 1)
            }
        }
        val model = HomeViewModel(source)
        try {
            model.loadExpenses("synthetic", 10, 2026, refreshKey = 1); runCurrent()
            model.loadExpenses("synthetic", 10, 2026, refreshKey = 2); runCurrent()
            assertEquals(2, calls)
            first.complete(Unit); runCurrent()
            assertTrue(model.uiState.value.expensesData.isEmpty())
            assertTrue(model.uiState.value.isExpensesLoading)
            second.complete(Unit); runCurrent()
            assertEquals(2, model.uiState.value.expensesData.single().id)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun paymentStatusChangeCannotReuseOrPublishThePreviousFilter() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        val fake = EditingFake()
        val statuses = mutableListOf<String?>()
        val source = object : HomeDataSource by fake {
            override suspend fun getEffectiveExpenses(token: String, month: Int, year: Int, paymentStatus: String?): ExpensesResponse {
                statuses += paymentStatus
                withContext(NonCancellable) { if (paymentStatus == "paid") first.await() else second.await() }
                return ExpensesResponse(listOf(testExpense(if (paymentStatus == "paid") 1 else 2)), 1)
            }
        }
        val model = HomeViewModel(source)
        try {
            model.loadExpenses("synthetic", 10, 2026, "paid"); runCurrent()
            model.loadExpenses("synthetic", 10, 2026, "pending"); runCurrent()
            assertEquals(listOf<String?>("paid", "pending"), statuses.toList())
            first.complete(Unit); runCurrent()
            assertTrue(model.uiState.value.expensesData.isEmpty())
            second.complete(Unit); runCurrent()
            assertEquals(2, model.uiState.value.expensesData.single().id)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun failedRefreshRetainsExpensesAndRetryKeepsTheErrorUntilSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = EditingFake()
        val model = DespesasViewModel(fake)
        try {
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            fake.readFailure = offline()
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertEquals(fake.scheduled, model.uiState.value.expensesData)
            assertNotNull(model.uiState.value.errorMessage)
            val gate = CompletableDeferred<Unit>()
            fake.readFailure = null
            fake.beforeRead = { gate.await() }
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertEquals(fake.scheduled, model.uiState.value.expensesData)
            assertTrue(model.uiState.value.isLoading)
            assertNotNull(model.uiState.value.errorMessage)
            gate.complete(Unit); runCurrent()
            assertNull(model.uiState.value.errorMessage)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun categoryAuthenticationFailureIsNeverTreatedAsASuccessfulSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = EditingFake()
        val source = object : ExpensesDataSource by fake {
            override suspend fun getCategories(token: String): CategoriesResponse = throw SessionExpiredException()
        }
        val model = DespesasViewModel(source)
        try {
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertTrue(model.uiState.value.isSessionExpired)
            assertNull(model.uiState.value.categoriesError)
        } finally { model.close(); Dispatchers.resetMain() }
    }
}
