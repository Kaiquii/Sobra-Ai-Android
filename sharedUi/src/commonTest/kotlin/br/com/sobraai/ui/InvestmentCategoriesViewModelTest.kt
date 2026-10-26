package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.patrimonio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.io.IOException
import kotlin.test.*

private class InvestmentCategoriesFake : InvestmentCategoriesDataSource {
    val categories = mutableListOf(InvestmentCategory(17, "CDB", true))
    var reads = 0
    var writes = 0
    var readFailure: Exception? = null
    var writeFailure: Exception? = null
    var loseWriteResponse = false
    var beforeRead: suspend () -> Unit = {}
    var beforeWrite: suspend () -> Unit = {}
    private var nextId = 301
    override suspend fun getCategories(token: String): InvestmentCategoriesResponse {
        reads++
        beforeRead()
        readFailure?.let { throw it }
        return InvestmentCategoriesResponse(categories.size, categories.toList())
    }
    private suspend fun writing() {
        writes++
        beforeWrite()
        writeFailure?.let { throw it }
    }
    private fun delivered() {
        if (loseWriteResponse) throw NetworkRequestException("Falha sintética", IOException("synthetic"))
    }
    override suspend fun createCategory(token: String, name: String): InvestmentCategory {
        writing()
        val category = InvestmentCategory(nextId++, name, false)
        categories += category
        delivered()
        return category
    }
    override suspend fun renameCategory(token: String, id: Int, name: String): InvestmentCategory {
        writing()
        val old = categories.single { it.id == id }
        val category = old.copy(name = name)
        categories[categories.indexOf(old)] = category
        delivered()
        return category
    }
    override suspend fun deleteCategory(token: String, id: Int) {
        writing()
        categories.removeAll { it.id == id }
        delivered()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InvestmentCategoriesViewModelTest {
    @Test fun crudUsesReturnedIdentityAndReloadsCatalogWithoutChangingMonthlyRevision() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        val monthlyRevision = FinanceDataChanges.revision.value
        try {
            model.load("synthetic"); runCurrent()
            assertEquals(true, model.state.value.supported)
            model.create("synthetic", "  Minha reserva  "); runCurrent()
            val created = model.state.value.categories.single { !it.is_default }
            assertEquals("Minha reserva", created.name)
            assertEquals(created.id, model.state.value.createdCategoryId)
            assertEquals(1L, model.state.value.mutationRevision)
            model.consumeCreatedCategory()
            assertNull(model.state.value.createdCategoryId)
            model.consumeSuccess()
            assertNull(model.state.value.successMessage)
            model.rename("synthetic", created, "  Nova categoria  "); runCurrent()
            val renamed = model.state.value.categories.single { it.id == created.id }
            assertEquals("Nova categoria", renamed.name)
            model.delete("synthetic", renamed); runCurrent()
            assertTrue(model.state.value.categories.all { it.is_default })
            assertEquals(3L, model.state.value.mutationRevision)
            assertEquals(3, source.writes)
            assertEquals(4, source.reads)
            assertEquals(monthlyRevision, FinanceDataChanges.revision.value)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun missingCategoryReportedByInvestmentForcesFreshCatalogBeforeItsCacheExpires() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        try {
            source.categories += InvestmentCategory(301, "Pessoal", false)
            model.load("synthetic"); runCurrent()
            source.categories.removeAll { it.id == 301 }
            model.load("synthetic"); runCurrent()
            assertEquals(1, source.reads)
            assertTrue(model.state.value.categories.any { it.id == 301 })
            model.refreshAfterInvestmentChange("synthetic"); runCurrent()
            assertEquals(2, source.reads)
            assertFalse(model.state.value.categories.any { it.id == 301 })
            model.refreshAfterInvestmentChange("another-synthetic"); runCurrent()
            assertEquals(2, source.reads)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun onlyCatalog404EnablesLegacyBackendContract() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (error in listOf(ApiResponseException(404, "Rota não encontrada."),
                ApiResponseException(403, "Acesso bloqueado."), ApiResponseException(503, "Indisponível."),
                NetworkRequestException("Falha sintética", IOException("synthetic")))) {
                val source = InvestmentCategoriesFake().apply { readFailure = error }
                val model = InvestmentCategoriesViewModel(source)
                try {
                    model.load("synthetic"); runCurrent()
                    if (error is ApiResponseException && error.statusCode == 404) {
                        assertEquals(false, model.state.value.supported)
                        assertNull(model.state.value.loadError)
                    } else {
                        assertNull(model.state.value.supported)
                        assertNotNull(model.state.value.loadError)
                    }
                    model.create("synthetic", "Pessoal"); runCurrent()
                    assertEquals(0, source.writes)
                } finally { model.close() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun reenteringImmediatelyDiscoversCatalogAfterLegacy404AndThenReusesSupportedResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake().apply {
            categories += InvestmentCategory(301, "Pessoal", false)
            readFailure = ApiResponseException(404, "Rota não encontrada.")
        }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            assertEquals(false, model.state.value.supported)
            assertEquals(1, source.reads)
            source.readFailure = null
            // Normal navigation, with neither an explicit retry nor a 60-second wait.
            model.load("synthetic"); runCurrent()
            assertEquals(true, model.state.value.supported)
            assertEquals(listOf(17, 301), model.state.value.categories.map { it.id })
            assertNull(model.state.value.loadError)
            assertEquals(2, source.reads)
            model.load("synthetic"); runCurrent()
            assertEquals(2, source.reads)
            assertEquals(listOf(17, 301), model.state.value.categories.map { it.id })
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun repeatedEntryDuringSlow404RecheckKeepsOneActiveRequestWithoutCancellation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake().apply { readFailure = ApiResponseException(404, "Rota não encontrada.") }
        val model = InvestmentCategoriesViewModel(source)
        val gate = CompletableDeferred<Unit>()
        var cancellations = 0
        try {
            model.load("synthetic"); runCurrent()
            assertEquals(false, model.state.value.supported)
            source.beforeRead = {
                try { gate.await() }
                finally { if (!currentCoroutineContext().isActive) cancellations++ }
            }
            model.load("synthetic"); runCurrent()
            assertTrue(model.state.value.loading)
            assertEquals(2, source.reads)
            model.load("synthetic"); runCurrent()
            model.load("synthetic"); runCurrent()
            assertTrue(model.state.value.loading)
            assertEquals(2, source.reads)
            assertEquals(0, cancellations)
            gate.complete(Unit); runCurrent()
            assertFalse(model.state.value.loading)
            assertEquals(false, model.state.value.supported)
            assertNull(model.state.value.loadError)
            assertEquals(2, source.reads)
            assertEquals(0, cancellations)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun networkFailureAfterLegacy404IsShownWithoutClaimingCatalogSupport() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake().apply { readFailure = ApiResponseException(404, "Rota não encontrada.") }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            assertEquals(false, model.state.value.supported)
            source.readFailure = NetworkRequestException("Falha sintética", IOException("synthetic"))
            model.load("synthetic"); runCurrent()
            assertEquals(2, source.reads)
            assertEquals(false, model.state.value.supported)
            assertEquals("Falha sintética", model.state.value.loadError)
            assertTrue(model.state.value.categories.isEmpty())
            model.create("synthetic", "Pessoal"); runCurrent()
            assertEquals(0, source.writes)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun unauthorizedAndExpiredSessionsClearCachedCategoriesRatherThanEnablingLegacy() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (error in listOf(SessionExpiredException(), ApiResponseException(401, "Sessão expirada."))) {
                val source = InvestmentCategoriesFake()
                val model = InvestmentCategoriesViewModel(source)
                try {
                    model.load("synthetic"); runCurrent()
                    assertTrue(model.state.value.categories.isNotEmpty())
                    source.readFailure = error
                    model.load("synthetic", retry = true); runCurrent()
                    assertTrue(model.state.value.isSessionExpired)
                    assertNull(model.state.value.supported)
                    assertTrue(model.state.value.categories.isEmpty())
                    model.create("synthetic", "Pessoal"); runCurrent()
                    assertEquals(0, source.writes)
                } finally { model.close() }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun blockedReadClearsPersonalCatalogAndIsNeverUnsupported() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake().apply { categories += InvestmentCategory(301, "Pessoal", false) }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            source.readFailure = ApiResponseException(403, "Acesso bloqueado.")
            model.load("synthetic", retry = true); runCurrent()
            assertTrue(model.state.value.categories.isEmpty())
            assertNull(model.state.value.supported)
            assertEquals("Acesso bloqueado.", model.state.value.loadError)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun defaultProtectionChecksBothCallerAndCurrentCatalogWithoutSendingWrites() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            val standard = model.state.value.categories.single()
            model.rename("synthetic", standard, "Alterado")
            assertNotNull(model.state.value.mutationError)
            model.clearMutationError()
            model.delete("synthetic", standard.copy(is_default = false)); runCurrent()
            assertNotNull(model.state.value.mutationError)
            assertEquals(0, source.writes)
            assertEquals("CDB", model.state.value.categories.single().name)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun conflictsAndServerProtectionKeepEditorErrorAndDoNotPublishSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake().apply { categories += InvestmentCategory(301, "Pessoal", false) }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            val personal = model.state.value.categories.single { !it.is_default }
            for (error in listOf(ApiResponseException(409, "Já existe uma categoria com este nome."),
                ApiResponseException(409, "Troque a categoria dos investimentos antes de excluir."),
                ApiResponseException(403, "Categorias padrão não podem ser alteradas."))) {
                source.writeFailure = error
                model.delete("synthetic", personal); runCurrent()
                assertEquals(error.apiMessage, model.state.value.mutationError)
                assertNull(model.state.value.successMessage)
                assertEquals(0L, model.state.value.mutationRevision)
                assertTrue(model.state.value.categories.any { it.id == personal.id })
                assertEquals(true, model.state.value.supported)
                model.clearMutationError()
            }
            assertEquals(1, source.reads)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun invalidNamesNeverSubmitAndUnicodeLengthCountsCharacters() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            for (name in listOf("", "  ", "x".repeat(81), "😀".repeat(81))) {
                model.create("synthetic", name); runCurrent()
                assertNotNull(model.state.value.mutationError)
            }
            assertEquals(0, source.writes)
            model.create("synthetic", "😀".repeat(80)); runCurrent()
            assertEquals(1, source.writes)
            assertNull(model.state.value.mutationError)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun returningDuringSlowReadReusesRequestAndDoesNotTurnLoadingIntoError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake().apply { beforeRead = { delay(15_000) } }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            advanceTimeBy(5_000)
            model.load("synthetic"); runCurrent()
            assertEquals(1, source.reads)
            assertTrue(model.state.value.loading)
            assertNull(model.state.value.loadError)
            model.create("synthetic", "Pessoal"); runCurrent()
            assertEquals(0, source.writes)
            advanceTimeBy(10_000); runCurrent()
            assertFalse(model.state.value.loading)
            assertEquals(true, model.state.value.supported)
            model.load("synthetic"); runCurrent()
            assertEquals(1, source.reads)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun failedRefreshRetainsLabeledCatalogButBlocksWritesUntilReadSucceeds() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        val gate = CompletableDeferred<Unit>()
        try {
            model.load("synthetic"); runCurrent()
            source.readFailure = NetworkRequestException("Falha sintética", IOException("synthetic"))
            model.load("synthetic", retry = true); runCurrent()
            assertEquals(1, model.state.value.categories.size)
            assertNotNull(model.state.value.loadError)
            model.create("synthetic", "Pessoal"); runCurrent()
            assertEquals(0, source.writes)
            source.readFailure = null
            source.beforeRead = { gate.await() }
            model.load("synthetic", retry = true); runCurrent()
            assertNotNull(model.state.value.loadError)
            gate.complete(Unit); runCurrent()
            assertNull(model.state.value.loadError)
            model.create("synthetic", "Pessoal"); runCurrent()
            assertEquals(1, source.writes)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun queuedDoubleCreatePerformsOneWriteAndUsesCreatedIdFromResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        val gate = CompletableDeferred<Unit>()
        try {
            model.load("synthetic"); runCurrent()
            source.beforeWrite = { gate.await() }
            model.create("synthetic", "Pessoal")
            model.create("synthetic", "Pessoal"); runCurrent()
            assertEquals(1, source.writes)
            gate.complete(Unit); runCurrent()
            assertEquals(301, model.state.value.createdCategoryId)
            assertEquals(2, source.reads)
            assertEquals(1L, model.state.value.mutationRevision)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun lostWriteResponseReconcilesCatalogWithoutDuplicatingOrClaimingSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = InvestmentCategoriesFake()
        val model = InvestmentCategoriesViewModel(source)
        val gate = CompletableDeferred<Unit>()
        try {
            model.load("synthetic"); runCurrent()
            source.loseWriteResponse = true
            source.beforeRead = { gate.await() }
            model.create("synthetic", "Pessoal"); runCurrent()
            assertTrue(model.state.value.loading)
            assertNotNull(model.state.value.reviewNotice)
            model.create("synthetic", "Pessoal"); runCurrent()
            assertEquals(1, source.writes)
            gate.complete(Unit); runCurrent()
            assertEquals(2, model.state.value.categories.size)
            assertEquals(2, source.reads)
            assertNull(model.state.value.createdCategoryId)
            assertNull(model.state.value.successMessage)
            assertNull(model.state.value.mutationError)
            assertEquals(0L, model.state.value.mutationRevision)
            model.consumeReviewNotice()
            assertNull(model.state.value.reviewNotice)
        } finally { gate.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun mutation404RefreshesItemsWithoutAssumingLegacyBackend() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val personal = InvestmentCategory(301, "Pessoal", false)
        val source = InvestmentCategoriesFake().apply { categories += personal }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("synthetic"); runCurrent()
            source.categories.remove(personal)
            source.writeFailure = ApiResponseException(404, "Categoria não encontrada.")
            model.delete("synthetic", personal); runCurrent()
            assertEquals(true, model.state.value.supported)
            assertEquals("Categoria não encontrada.", model.state.value.mutationError)
            assertFalse(model.state.value.categories.any { it.id == personal.id })
            assertEquals(2, source.reads)
            assertEquals(0L, model.state.value.mutationRevision)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun lateReadFromPreviousAccountCannotPublishPersonalCategories() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val old = CompletableDeferred<Unit>()
        val fake = InvestmentCategoriesFake()
        val source = object : InvestmentCategoriesDataSource by fake {
            override suspend fun getCategories(token: String): InvestmentCategoriesResponse {
                if (token == "old-synthetic") withContext(NonCancellable) { old.await() }
                return InvestmentCategoriesResponse(1, listOf(InvestmentCategory(301, token, false)))
            }
        }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("old-synthetic"); runCurrent()
            model.load("new-synthetic"); runCurrent()
            assertEquals("new-synthetic", model.state.value.categories.single().name)
            old.complete(Unit); runCurrent()
            assertEquals("new-synthetic", model.state.value.categories.single().name)
            assertNull(model.state.value.createdCategoryId)
        } finally { old.complete(Unit); model.close(); Dispatchers.resetMain() }
    }

    @Test fun lateWriteFromPreviousAccountCannotPublishCreatedIdOrRefreshNewAccount() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val old = CompletableDeferred<Unit>()
        val reads = mutableListOf<String>()
        val fake = InvestmentCategoriesFake()
        val source = object : InvestmentCategoriesDataSource by fake {
            override suspend fun getCategories(token: String): InvestmentCategoriesResponse {
                reads += token
                return InvestmentCategoriesResponse(1, listOf(InvestmentCategory(301, token, false)))
            }
            override suspend fun createCategory(token: String, name: String): InvestmentCategory {
                withContext(NonCancellable) { old.await() }
                return InvestmentCategory(888, name, false)
            }
        }
        val model = InvestmentCategoriesViewModel(source)
        try {
            model.load("old-synthetic"); runCurrent()
            model.create("old-synthetic", "Pessoal"); runCurrent()
            assertTrue(model.state.value.saving)
            model.load("new-synthetic"); runCurrent()
            assertFalse(model.state.value.saving)
            old.complete(Unit); runCurrent()
            assertEquals("new-synthetic", model.state.value.categories.single().name)
            assertNull(model.state.value.createdCategoryId)
            assertNull(model.state.value.successMessage)
            assertEquals(0L, model.state.value.mutationRevision)
            assertEquals(listOf("old-synthetic", "new-synthetic"), reads.toList())
        } finally { old.complete(Unit); model.close(); Dispatchers.resetMain() }
    }
}
