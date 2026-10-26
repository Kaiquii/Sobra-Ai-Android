package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.patrimonio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.io.IOException
import kotlin.test.*

private class PatrimonyFake : PatrimonyDataSource {
    var fgts: FgtsBalance? = null
    val investments = mutableListOf<Investment>()
    var reads = 0
    var writes = 0
    var lastInvestmentRequest: InvestmentWriteRequest? = null
    var readFailure: Exception? = null
    var loseWriteResponse = false
    var beforeRead: suspend () -> Unit = {}
    var beforeWrite: suspend () -> Unit = {}
    private val timestamp = "2026-10-26T12:00:00Z"
    fun snapshot(): PatrimonyResponse {
        val a = fgts?.amount ?: PatrimonyAmount.Zero
        val b = PatrimonyAmount(investments.sumOf { it.amount.cents })
        return PatrimonyResponse(fgts, investments.toList(), a, b, PatrimonyAmount(a.cents + b.cents))
    }
    override suspend fun getPatrimony(token: String): PatrimonyResponse {
        reads++; beforeRead(); readFailure?.let { throw it }; return snapshot()
    }
    private fun delivered() { if (loseWriteResponse) throw NetworkRequestException("Falha sintética", IOException("synthetic")) }
    override suspend fun updateFgts(token: String, amount: PatrimonyAmount): FgtsBalance {
        writes++; beforeWrite()
        val result = FgtsBalance(amount, timestamp); fgts = result; delivered(); return result
    }
    override suspend fun deleteFgts(token: String) {
        writes++; beforeWrite()
        if (fgts == null) throw ApiResponseException(404, "FGTS não encontrado.")
        fgts = null; delivered()
    }
    override suspend fun createInvestment(token: String, request: InvestmentWriteRequest): Investment {
        writes++; beforeWrite()
        val investment = Investment((investments.maxOfOrNull { it.id } ?: 0) + 1,
            request.name, request.category ?: "Categoria sintética", request.amount, timestamp, timestamp, request.category_id)
        lastInvestmentRequest = request
        investments += investment; delivered(); return investment
    }
    override suspend fun updateInvestment(token: String, id: Int, request: InvestmentWriteRequest): Investment {
        writes++; beforeWrite()
        val previous = investments.single { it.id == id }
        val updated = previous.copy(name = request.name, category = request.category ?: "Categoria sintética",
            amount = request.amount, category_id = request.category_id)
        lastInvestmentRequest = request
        investments[investments.indexOf(previous)] = updated; delivered(); return updated
    }
    override suspend fun deleteInvestment(token: String, id: Int) {
        writes++; beforeWrite()
        if (!investments.removeAll { it.id == id }) throw ApiResponseException(404, "Investimento não encontrado.")
        delivered()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatrimonyViewModelTest {
    @Test fun crudRefreshesTotalsPreservesZeroAndDoesNotInvalidateMonthlyFinances() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val model = PatrimonyViewModel(fake)
        val monthlyRevision = FinanceDataChanges.revision.value
        try {
            model.load("synthetic"); runCurrent()
            assertNull(model.state.value.data?.fgts)
            assertEquals(PatrimonyAmount.Zero, model.state.value.data?.total_patrimony)
            model.saveFgts("synthetic", "0"); runCurrent()
            assertNotNull(model.state.value.data?.fgts)
            assertEquals(PatrimonyAmount.Zero, model.state.value.data?.fgts?.amount)
            model.saveFgts("synthetic", "2.500,50"); runCurrent()
            model.saveInvestment("synthetic", null, " Reserva ", " CDB ", "5000"); runCurrent()
            assertEquals(PatrimonyAmount(750050), model.state.value.data?.total_patrimony)
            val id = assertNotNull(model.state.value.data).investments.single().id
            model.saveInvestment("synthetic", id, "Reserva editada", "Tesouro", "0,01"); runCurrent()
            assertEquals(PatrimonyAmount(250051), model.state.value.data?.total_patrimony)
            assertEquals("Tesouro", model.state.value.data?.investments?.single()?.category)
            model.deleteInvestment("synthetic", id); runCurrent()
            model.deleteFgts("synthetic"); runCurrent()
            assertTrue(assertNotNull(model.state.value.data).investments.isEmpty())
            assertNull(model.state.value.data?.fgts)
            assertEquals(6, fake.writes)
            assertEquals(7, fake.reads)
            assertEquals(monthlyRevision, FinanceDataChanges.revision.value)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun investmentSaveUsesTheSelectedCatalogIdWithoutSendingTheOldLabel() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            model.saveInvestment("synthetic", null, "Reserva", "Rótulo antigo", "1,01", categoryId = 37); runCurrent()
            assertEquals(37, fake.lastInvestmentRequest?.category_id)
            assertNull(fake.lastInvestmentRequest?.category)
            assertEquals(37, model.state.value.data?.investments?.single()?.category_id)
            model.saveInvestment("synthetic", 1, "Reserva", "Rótulo antigo", "2,02", categoryId = 38); runCurrent()
            assertEquals(38, fake.lastInvestmentRequest?.category_id)
            assertNull(fake.lastInvestmentRequest?.category)
            assertEquals(PatrimonyAmount(202), model.state.value.data?.total_investments)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun reopeningReadsPersistedBalancesRatherThanStartingFromLocalDefaults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val first = PatrimonyViewModel(fake)
        val second = PatrimonyViewModel(fake)
        try {
            first.load("synthetic"); runCurrent()
            first.saveFgts("synthetic", "100,01"); runCurrent()
            first.close()
            second.load("synthetic"); runCurrent()
            assertEquals(PatrimonyAmount(10001), second.state.value.data?.total_fgts)
            assertEquals(3, fake.reads)
        } finally { first.close(); second.close(); Dispatchers.resetMain() }
    }

    @Test fun validationFailuresNeverSubmitWrites() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            for (value in listOf("", "-1", "1,001", "1e9", "10.000.000.000,00")) {
                model.saveFgts("synthetic", value)
                assertNotNull(model.state.value.mutationError)
            }
            model.saveInvestment("synthetic", null, "", "CDB", "1")
            model.saveInvestment("synthetic", null, "Reserva", "", "1")
            model.saveInvestment("synthetic", null, "x".repeat(121), "CDB", "1")
            runCurrent()
            assertEquals(0, fake.writes)
            assertNotNull(model.state.value.mutationError)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun failedRefreshRetainsDataAndRetryDoesNotClearTheWarningUntilSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake().apply { fgts = FgtsBalance(PatrimonyAmount(100), "2026-10-26T12:00:00Z") }
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            fake.readFailure = NetworkRequestException("Falha sintética", IOException("synthetic"))
            model.load("synthetic", retry = true); runCurrent()
            assertEquals(PatrimonyAmount(100), model.state.value.data?.total_fgts)
            assertNotNull(model.state.value.loadError)
            val gate = CompletableDeferred<Unit>()
            fake.readFailure = null; fake.beforeRead = { gate.await() }
            model.load("synthetic", retry = true); runCurrent()
            assertTrue(model.state.value.loading)
            assertNotNull(model.state.value.loadError)
            gate.complete(Unit); runCurrent()
            assertNull(model.state.value.loadError)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun returningWhileReadIsSlowKeepsTheSameRequestLoadingUntilItFinishes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake().apply { beforeRead = { delay(15_000) } }
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            advanceTimeBy(5_000)
            model.load("synthetic"); runCurrent()
            assertTrue(model.state.value.loading)
            assertNull(model.state.value.loadError)
            assertEquals(1, fake.reads)
            advanceTimeBy(10_000); runCurrent()
            assertFalse(model.state.value.loading)
            assertNull(model.state.value.loadError)
            assertEquals(PatrimonyAmount.Zero, model.state.value.data?.total_patrimony)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun initialFailureIsNotTreatedAsZeroAndExplicitRetryShowsLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake().apply { readFailure = ApiResponseException(503, "Serviço indisponível.") }
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            assertNull(model.state.value.data)
            assertEquals("Serviço indisponível.", model.state.value.loadError)
            val gate = CompletableDeferred<Unit>()
            fake.readFailure = null; fake.beforeRead = { gate.await() }
            model.load("synthetic", retry = true); runCurrent()
            assertTrue(model.state.value.loading)
            assertNull(model.state.value.loadError)
            gate.complete(Unit); runCurrent()
            assertEquals(PatrimonyAmount.Zero, model.state.value.data?.total_patrimony)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun savingDoesNotRepeatTheWriteAndForcesGetAfterSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val gate = CompletableDeferred<Unit>()
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            fake.beforeWrite = { gate.await() }
            model.saveFgts("synthetic", "10")
            model.saveFgts("synthetic", "20"); runCurrent()
            assertEquals(1, fake.writes)
            gate.complete(Unit); runCurrent()
            assertEquals(2, fake.reads)
            assertEquals(PatrimonyAmount(1000), model.state.value.data?.total_fgts)
            assertEquals(1L, model.state.value.mutationRevision)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun lostWriteResponseReconcilesWithGetWithoutCreatingADuplicate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            fake.loseWriteResponse = true
            model.saveInvestment("synthetic", null, "Reserva", "CDB", "1,01"); runCurrent()
            assertEquals(1, fake.writes)
            assertEquals(2, fake.reads)
            assertEquals(1, model.state.value.data?.investments?.size)
            assertNull(model.state.value.mutationError)
            assertNotNull(model.state.value.reviewNotice)
            model.consumeReviewNotice()
            assertNull(model.state.value.reviewNotice)
            assertNull(model.state.value.successMessage)
            assertEquals(0L, model.state.value.mutationRevision)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun pendingReconciliationAndFailedReadsNeverPermitAnotherWrite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val model = PatrimonyViewModel(fake)
        val gate = CompletableDeferred<Unit>()
        try {
            model.load("synthetic"); runCurrent()
            fake.loseWriteResponse = true
            fake.beforeRead = { gate.await() }
            model.saveInvestment("synthetic", null, "Reserva", "CDB", "1,01"); runCurrent()
            assertTrue(model.state.value.loading)
            assertNotNull(model.state.value.reviewNotice)
            model.saveInvestment("synthetic", null, "Reserva", "CDB", "1,01"); runCurrent()
            assertEquals(1, fake.writes)
            fake.readFailure = NetworkRequestException("Falha sintética", IOException("synthetic"))
            gate.complete(Unit); runCurrent()
            assertNotNull(model.state.value.loadError)
            model.saveInvestment("synthetic", null, "Reserva", "CDB", "1,01"); runCurrent()
            assertEquals(1, fake.writes)
            fake.readFailure = null
            model.load("synthetic", retry = true); runCurrent()
            assertEquals(1, model.state.value.data?.investments?.size)
            assertEquals(PatrimonyAmount(101), model.state.value.data?.total_investments)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun confirmedWriteWithFailedRefreshKeepsLastDataLabeledAsStale() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake = PatrimonyFake()
        val model = PatrimonyViewModel(fake)
        try {
            model.load("synthetic"); runCurrent()
            fake.readFailure = ApiResponseException(503, "Serviço indisponível.")
            model.saveFgts("synthetic", "10"); runCurrent()
            assertNotNull(model.state.value.successMessage)
            assertEquals(PatrimonyAmount.Zero, model.state.value.data?.total_patrimony)
            assertNotNull(model.state.value.loadError)
            fake.readFailure = null
            model.load("synthetic", retry = true); runCurrent()
            assertEquals(PatrimonyAmount(1000), model.state.value.data?.total_patrimony)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun expiredOrBlockedAccessCannotLeaveCachedPatrimonyVisible() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (failure in listOf(SessionExpiredException(), ApiResponseException(403, "Acesso bloqueado."))) {
                val fake = PatrimonyFake()
                val model = PatrimonyViewModel(fake)
                try {
                    model.load("synthetic"); runCurrent()
                    fake.readFailure = failure
                    model.load("synthetic", retry = true); runCurrent()
                    assertNull(model.state.value.data)
                    if (failure is SessionExpiredException) assertTrue(model.state.value.isSessionExpired)
                    else assertEquals("Acesso bloqueado.", model.state.value.loadError)
                } finally { model.close() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun lateReadFromPreviousAccountCannotPublishSensitiveData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val old = CompletableDeferred<Unit>()
        val fake = PatrimonyFake()
        val source = object : PatrimonyDataSource by fake {
            override suspend fun getPatrimony(token: String): PatrimonyResponse {
                if (token == "old-synthetic") withContext(NonCancellable) { old.await() }
                val amount = PatrimonyAmount(if (token == "old-synthetic") 9999 else 100)
                return PatrimonyResponse(FgtsBalance(amount, "2026-10-26T12:00:00Z"), emptyList(),
                    amount, PatrimonyAmount.Zero, amount)
            }
        }
        val model = PatrimonyViewModel(source)
        try {
            model.load("old-synthetic"); runCurrent()
            model.load("new-synthetic"); runCurrent()
            assertEquals(PatrimonyAmount(100), model.state.value.data?.total_fgts)
            old.complete(Unit); runCurrent()
            assertEquals(PatrimonyAmount(100), model.state.value.data?.total_fgts)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun timestampsWithDifferentOffsetsAndPrecisionDisplayTheSameInstant() {
        assertEquals(patrimonyUpdatedAt("2026-10-26T12:00:00Z"), patrimonyUpdatedAt("2026-10-26T09:00:00.000000000-03:00"))
        assertEquals("data indisponível", patrimonyUpdatedAt("invalid"))
    }

    @Test fun lateWriteFromPreviousAccountCannotPublishSuccessOrReloadTheNewAccount() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val oldWrite = CompletableDeferred<Unit>()
        val fake = PatrimonyFake()
        val readTokens = mutableListOf<String>()
        val source = object : PatrimonyDataSource by fake {
            override suspend fun getPatrimony(token: String): PatrimonyResponse {
                readTokens += token
                val amount = PatrimonyAmount(if (token == "old-synthetic") 100 else 200)
                return PatrimonyResponse(FgtsBalance(amount, "2026-10-26T12:00:00Z"), emptyList(),
                    amount, PatrimonyAmount.Zero, amount)
            }
            override suspend fun updateFgts(token: String, amount: PatrimonyAmount): FgtsBalance {
                withContext(NonCancellable) { oldWrite.await() }
                return FgtsBalance(amount, "2026-10-26T12:00:00Z")
            }
        }
        val model = PatrimonyViewModel(source)
        try {
            model.load("old-synthetic"); runCurrent()
            model.saveFgts("old-synthetic", "10"); runCurrent()
            assertTrue(model.state.value.saving)
            model.load("new-synthetic"); runCurrent()
            assertFalse(model.state.value.saving)
            oldWrite.complete(Unit); runCurrent()
            assertEquals(PatrimonyAmount(200), model.state.value.data?.total_fgts)
            assertNull(model.state.value.successMessage)
            assertEquals(listOf("old-synthetic", "new-synthetic"), readTokens.toList())
        } finally { model.close(); Dispatchers.resetMain() }
    }
}
