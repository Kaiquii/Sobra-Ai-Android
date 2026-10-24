package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.feature.despesas.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesLoadNoticesTest {
    @Test fun matchingTransportFailuresHaveOneNoticeAndRetryBothQueries() {
        val state = DespesasUiState(
            expensesData = listOf(testExpense()),
            errorMessage = "Falha sintética", categoriesError = "Falha sintética",
            expensesNetworkFailure = NetworkFailureKind.Connection,
            categoriesNetworkFailure = NetworkFailureKind.Connection,
            isLoading = false
        )
        val notice = expensesLoadNotices(state).single()
        assertEquals(ExpensesRetryTarget.Both, notice.target)
        assertTrue(notice.message.contains("servidor"))
        assertTrue(notice.message.contains("dados salvos"))
        assertTrue(expensesLoadNotices(state.copy(isCategoriesLoading = true)).single().retrying)
        // Recovery of one endpoint must leave the other endpoint's failure visible.
        val remaining = expensesLoadNotices(state.copy(categoriesError = null, categoriesNetworkFailure = null)).single()
        assertEquals(ExpensesRetryTarget.Expenses, remaining.target)
    }

    @Test fun unrelatedServerFailuresAreNeverMergedOrDescribedAsOffline() {
        val state = DespesasUiState(expensesData = listOf(testExpense()), categoriesMap = mapOf(1 to "Casa"),
            errorMessage = "Não foi possível conectar ao serviço de despesas.", categoriesError = "Falha nas categorias.")
        val notices = expensesLoadNotices(state)
        assertEquals(2, notices.size)
        assertTrue(notices.any { it.message.contains("Não foi possível conectar ao serviço de despesas.") })
        assertFalse(notices.any { it.message.contains("Falha de conexão com o servidor") })
        assertFalse(notices.any { it.message.contains("Sem conexão") })
        val mismatched = state.copy(expensesNetworkFailure = NetworkFailureKind.Connection,
            categoriesNetworkFailure = NetworkFailureKind.Timeout)
        assertEquals(2, expensesLoadNotices(mismatched).size)
    }

    @Test fun timeoutIsNotReportedAsMissingInternet() {
        val timeout = "O servidor demorou para responder. Tente novamente."
        val message = dataRequestErrorMessage(timeout, true)
        assertTrue(message.contains("demorou"))
        assertFalse(message.contains("Sem conexão"))
        val combined = expensesLoadNotices(DespesasUiState(errorMessage = timeout, categoriesError = timeout,
            expensesNetworkFailure = NetworkFailureKind.Timeout, categoriesNetworkFailure = NetworkFailureKind.Timeout)).single()
        assertTrue(combined.message.contains("demorou"))
    }

    @Test fun realFailuresRemainUntilSuccessfulReadsAndRecoveryClearsTheirClassification() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = EditingFake()
        val model = DespesasViewModel(source)
        try {
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            source.readFailure = NetworkRequestException("Falha sintética de conexão", IllegalStateException("synthetic"))
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertEquals(1, expensesLoadNotices(model.uiState.value).size)
            val gate = CompletableDeferred<Unit>()
            source.readFailure = null; source.beforeRead = { gate.await() }
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertNotNull(model.uiState.value.errorMessage)
            assertTrue(expensesLoadNotices(model.uiState.value).single().retrying)
            gate.complete(Unit); runCurrent()
            assertTrue(expensesLoadNotices(model.uiState.value).isEmpty())
            assertNull(model.uiState.value.expensesNetworkFailure)
            assertNull(model.uiState.value.categoriesNetworkFailure)
            source.readFailure = ApiResponseException(503, "Serviço indisponível.")
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            assertEquals(2, expensesLoadNotices(model.uiState.value).size)
            assertTrue(model.uiState.value.errorMessage!!.contains("Serviço indisponível"))
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun lateFailureFromCancelledPeriodCannotReplaceSuccessfulCurrentPeriod() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val source = EditingFake().apply {
            beforeRead = { month ->
                if (month == 10) withContext(NonCancellable) {
                    gate.await()
                    throw NetworkRequestException("Falha antiga", IllegalStateException("synthetic"))
                }
            }
        }
        val model = DespesasViewModel(source)
        try {
            model.loadExpenses("synthetic", 10, 2026); runCurrent()
            model.loadExpenses("synthetic", 11, 2026); runCurrent()
            gate.complete(Unit); runCurrent()
            assertNull(model.uiState.value.errorMessage)
            assertNull(model.uiState.value.expensesNetworkFailure)
            assertFalse(model.uiState.value.isLoading)
            assertTrue(expensesLoadNotices(model.uiState.value).isEmpty())
        } finally { model.close(); Dispatchers.resetMain() }
    }
}
