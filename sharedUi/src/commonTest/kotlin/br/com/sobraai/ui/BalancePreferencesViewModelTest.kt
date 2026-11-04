package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.data.BalancePreferencesDataSource
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.network.BalancePreferences
import com.example.appfinanceiro.core.network.BalancePreferencesUpdate
import com.example.appfinanceiro.core.network.SessionAccessEvents
import com.example.appfinanceiro.feature.perfil.BalanceBenefit
import com.example.appfinanceiro.feature.perfil.BalancePreferencesState
import com.example.appfinanceiro.feature.perfil.BalancePreferencesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class BalancePreferencesFake : BalancePreferencesDataSource {
    var preferences = BalancePreferences(false, false)
    val reads = mutableListOf<String>()
    val writes = mutableListOf<Pair<String, BalancePreferencesUpdate>>()
    var readFailure: Exception? = null
    var writeFailure: Exception? = null
    var writeResponse: BalancePreferences? = null
    var beforeRead: suspend (String) -> Unit = {}
    var beforeWrite: suspend (String) -> Unit = {}

    override suspend fun getBalancePreferences(token: String): BalancePreferences {
        reads += token
        beforeRead(token)
        readFailure?.let { throw it }
        return preferences
    }

    override suspend fun updateBalancePreferences(token: String, request: BalancePreferencesUpdate): BalancePreferences {
        writes += token to request
        beforeWrite(token)
        writeFailure?.let { throw it }
        preferences = writeResponse ?: BalancePreferences(
            request.exclude_va_from_available ?: preferences.exclude_va_from_available,
            request.exclude_vr_from_available ?: preferences.exclude_vr_from_available
        )
        return preferences
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BalancePreferencesViewModelTest {
    private fun TestScope.withMain(block: TestScope.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun allFourServerChoicesAreRetainedAcrossNavigationAndReadAgainOnExplicitRefresh() = runTest { withMain {
        for (va in listOf(false, true)) for (vr in listOf(false, true)) {
            val source = BalancePreferencesFake().apply { preferences = BalancePreferences(va, vr) }
            val model = BalancePreferencesViewModel(source)
            try {
                assertNull(model.state.value.preferences)
                assertFalse(model.state.value.canEdit)
                model.load("synthetic"); runCurrent()
                assertEquals(BalancePreferences(va, vr), model.state.value.preferences)
                assertTrue(model.state.value.canEdit)
                repeat(3) { model.load("synthetic"); runCurrent() }
                assertEquals(listOf("synthetic"), source.reads)
                model.load("synthetic", force = true); runCurrent()
                assertEquals(2, source.reads.size)
                assertEquals(BalancePreferences(va, vr), model.state.value.preferences)
                assertTrue(source.writes.isEmpty())
            } finally { model.close() }
        }
    } }

    @Test fun partialFalsePatchesPreserveOtherBenefitAndOnlyConfirmedResponsesInvalidateFinances() = runTest { withMain {
        for (benefit in BalanceBenefit.entries) {
            val source = BalancePreferencesFake().apply { preferences = BalancePreferences(true, true) }
            val model = BalancePreferencesViewModel(source)
            val gate = CompletableDeferred<Unit>()
            val revision = FinanceDataChanges.revision.value
            try {
                model.load("synthetic"); runCurrent()
                source.beforeWrite = { gate.await() }
                model.setExcluded("synthetic", benefit, false)
                model.setExcluded("synthetic", benefit, false)
                model.setExcluded("synthetic", if (benefit == BalanceBenefit.VA) BalanceBenefit.VR else BalanceBenefit.VA, false)
                runCurrent()
                assertEquals(benefit, model.state.value.saving)
                assertFalse(model.state.value.canEdit)
                assertEquals(BalancePreferences(true, true), model.state.value.preferences)
                assertEquals(revision, FinanceDataChanges.revision.value)
                val expected = if (benefit == BalanceBenefit.VA) BalancePreferencesUpdate(exclude_va_from_available = false)
                    else BalancePreferencesUpdate(exclude_vr_from_available = false)
                assertEquals(listOf("synthetic" to expected), source.writes)
                // Returning to the screen, including a retry action, cannot cancel or race the PATCH.
                model.load("synthetic")
                model.load("synthetic", force = true); runCurrent()
                assertEquals(1, source.reads.size)
                assertEquals(benefit, model.state.value.saving)
                gate.complete(Unit); runCurrent()
                val confirmed = if (benefit == BalanceBenefit.VA) BalancePreferences(false, true) else BalancePreferences(true, false)
                assertEquals(confirmed, model.state.value.preferences)
                assertEquals(confirmed, source.preferences)
                assertEquals(revision + 1, FinanceDataChanges.revision.value)
                assertTrue(model.state.value.canEdit)
                assertNull(model.state.value.error)
                model.setExcluded("synthetic", benefit, false); runCurrent()
                assertEquals(1, source.writes.size)
                assertEquals(revision + 1, FinanceDataChanges.revision.value)
            } finally { gate.complete(Unit); model.close() }
        }
    } }

    @Test fun successfulPatchUsesTheFullServerResponseAndANewModelReadsThePersistedChoice() = runTest { withMain {
        val source = BalancePreferencesFake().apply { writeResponse = BalancePreferences(true, true) }
        val first = BalancePreferencesViewModel(source)
        val reopened = BalancePreferencesViewModel(source)
        try {
            first.load("synthetic"); runCurrent()
            first.setExcluded("synthetic", BalanceBenefit.VA, true); runCurrent()
            assertEquals(BalancePreferences(true, true), first.state.value.preferences)
            assertEquals(BalancePreferencesUpdate(exclude_va_from_available = true), source.writes.single().second)
            first.close()
            reopened.load("synthetic"); runCurrent()
            assertEquals(BalancePreferences(true, true), reopened.state.value.preferences)
            assertEquals(2, source.reads.size)
            assertEquals(1, source.writes.size)
        } finally { first.close(); reopened.close() }
    } }

    @Test fun initialReadFailuresNeverInventDefaultsOrPermitWritesAndRetryIsExplicit() = runTest { withMain {
        val failures = listOf(ApiResponseException(404, "Rota indisponível."), ApiResponseException(503, "Serviço indisponível."),
            NetworkRequestException("Falha sintética", IllegalStateException("synthetic")), IllegalArgumentException("synthetic JSON"))
        for (failure in failures) {
            val source = BalancePreferencesFake().apply { readFailure = failure }
            val model = BalancePreferencesViewModel(source)
            try {
                model.load("synthetic"); runCurrent()
                assertNull(model.state.value.preferences)
                assertTrue(model.state.value.loadFailed)
                assertFalse(model.state.value.canEdit)
                assertNotNull(model.state.value.error)
                model.setExcluded("synthetic", BalanceBenefit.VA, true)
                model.setExcluded("synthetic", BalanceBenefit.VR, false)
                source.readFailure = null
                model.load("synthetic"); runCurrent()
                assertEquals(1, source.reads.size)
                assertTrue(source.writes.isEmpty())
                assertNull(model.state.value.preferences)
                model.load("synthetic", force = true); runCurrent()
                assertEquals(2, source.reads.size)
                assertEquals(BalancePreferences(false, false), model.state.value.preferences)
                assertTrue(model.state.value.canEdit)
                assertNull(model.state.value.error)
            } finally { model.close() }
        }
    } }

    @Test fun failedRefreshKeepsConfirmedChoicesVisibleButRequiresSuccessfulReadBeforeWriting() = runTest { withMain {
        val source = BalancePreferencesFake().apply { preferences = BalancePreferences(true, false) }
        val model = BalancePreferencesViewModel(source)
        val gate = CompletableDeferred<Unit>()
        try {
            model.load("synthetic"); runCurrent()
            source.readFailure = ApiResponseException(503, "Serviço indisponível.")
            model.load("synthetic", force = true); runCurrent()
            assertEquals(BalancePreferences(true, false), model.state.value.preferences)
            assertFalse(model.state.value.canEdit)
            assertEquals("Serviço indisponível.", model.state.value.error)
            model.setExcluded("synthetic", BalanceBenefit.VA, false); runCurrent()
            assertTrue(source.writes.isEmpty())
            source.readFailure = null
            source.beforeRead = { gate.await() }
            model.load("synthetic", force = true); runCurrent()
            assertTrue(model.state.value.loading)
            assertEquals("Serviço indisponível.", model.state.value.error)
            model.load("synthetic", force = true); runCurrent()
            assertEquals(3, source.reads.size)
            gate.complete(Unit); runCurrent()
            assertNull(model.state.value.error)
            assertFalse(model.state.value.loadFailed)
            assertTrue(model.state.value.canEdit)
        } finally { gate.complete(Unit); model.close() }
    } }

    @Test fun patchFailurePreservesConfirmedChoiceWithoutRetryOrInvalidationAndAllowsExplicitAttempt() = runTest { withMain {
        val source = BalancePreferencesFake().apply { preferences = BalancePreferences(false, true) }
        val model = BalancePreferencesViewModel(source)
        val revision = FinanceDataChanges.revision.value
        try {
            model.load("synthetic"); runCurrent()
            source.writeFailure = NetworkRequestException("Falha sintética", IllegalStateException("synthetic"))
            model.setExcluded("synthetic", BalanceBenefit.VA, true); runCurrent()
            assertEquals(BalancePreferences(false, true), model.state.value.preferences)
            assertEquals("Falha sintética", model.state.value.error)
            assertNull(model.state.value.saving)
            assertTrue(model.state.value.canEdit)
            model.load("synthetic"); runCurrent()
            assertEquals(1, source.reads.size)
            assertEquals(1, source.writes.size)
            assertEquals(revision, FinanceDataChanges.revision.value)
            source.writeFailure = null
            model.setExcluded("synthetic", BalanceBenefit.VA, true); runCurrent()
            assertEquals(2, source.writes.size)
            assertEquals(BalancePreferences(true, true), model.state.value.preferences)
            assertEquals(revision + 1, FinanceDataChanges.revision.value)
            assertNull(model.state.value.error)
        } finally { model.close() }
    } }

    @Test fun pendingReadsAreReusedAndCannotRaceWithWritesEvenBeforeTheReadCoroutineStarts() = runTest { withMain {
        val gate = CompletableDeferred<Unit>()
        var cancellations = 0
        val source = BalancePreferencesFake().apply {
            beforeRead = { try { gate.await() } finally { if (!currentCoroutineContext().isActive) cancellations++ } }
        }
        val model = BalancePreferencesViewModel(source)
        try {
            model.load("synthetic")
            model.setExcluded("synthetic", BalanceBenefit.VA, true); runCurrent()
            model.load("synthetic")
            model.load("synthetic", force = true)
            model.setExcluded("synthetic", BalanceBenefit.VR, true); runCurrent()
            assertTrue(model.state.value.loading)
            assertEquals(1, source.reads.size)
            assertTrue(source.writes.isEmpty())
            assertEquals(0, cancellations)
            gate.complete(Unit); runCurrent()
            assertTrue(model.state.value.canEdit)
            assertFalse(model.state.value.loading)
        } finally { gate.complete(Unit); model.close() }
    } }

    @Test fun lateReadsCannotRestoreLoggedOutOrPreviousAccountChoicesIncludingReturningToTheSameToken() = runTest { withMain {
        for (replacement in listOf<String?>(null, "new-synthetic", "old-synthetic")) {
            val oldRead = CompletableDeferred<Unit>()
            var count = 0
            val source = object : BalancePreferencesDataSource by BalancePreferencesFake() {
                override suspend fun getBalancePreferences(token: String): BalancePreferences {
                    count++
                    if (count == 1) {
                        withContext(NonCancellable) { oldRead.await() }
                        return BalancePreferences(true, true)
                    }
                    return BalancePreferences(false, true)
                }
            }
            val model = BalancePreferencesViewModel(source)
            val revision = FinanceDataChanges.revision.value
            try {
                model.load("old-synthetic"); runCurrent()
                model.load(null)
                assertEquals(BalancePreferencesState(), model.state.value)
                if (replacement != null) { model.load(replacement); runCurrent() }
                oldRead.complete(Unit); runCurrent()
                assertEquals(if (replacement == null) null else BalancePreferences(false, true), model.state.value.preferences)
                assertFalse(model.state.value.loading)
                assertNull(model.state.value.error)
                assertEquals(revision, FinanceDataChanges.revision.value)
            } finally { oldRead.complete(Unit); model.close() }
        }
    } }

    @Test fun latePatchSuccessCannotPublishOrInvalidateAfterLogoutOrAccountChange() = runTest { withMain {
        for (replacement in listOf<String?>(null, "new-synthetic", "old-synthetic")) {
            val oldWrite = CompletableDeferred<Unit>()
            val source = BalancePreferencesFake().apply { beforeWrite = { withContext(NonCancellable) { oldWrite.await() } } }
            val model = BalancePreferencesViewModel(source)
            val revision = FinanceDataChanges.revision.value
            try {
                model.load("old-synthetic"); runCurrent()
                model.setExcluded("old-synthetic", BalanceBenefit.VA, true); runCurrent()
                assertEquals(BalanceBenefit.VA, model.state.value.saving)
                model.load(null)
                if (replacement != null) { model.load(replacement); runCurrent() }
                oldWrite.complete(Unit); runCurrent()
                assertEquals(if (replacement == null) null else BalancePreferences(false, false), model.state.value.preferences)
                assertNull(model.state.value.saving)
                assertNull(model.state.value.error)
                assertEquals(revision, FinanceDataChanges.revision.value)
                assertEquals(1, source.writes.size)
                assertEquals(if (replacement == null) 1 else 2, source.reads.size)
            } finally { oldWrite.complete(Unit); model.close() }
        }
    } }

    @Test fun activeSessionExpirationClearsChoicesAndNotifiesTheSessionControllerForGetAndPatch() = runTest { withMain {
        val events = mutableListOf<String>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { SessionAccessEvents.accessRevoked.collect { events += it } }
        try {
            for (patch in listOf(false, true)) for (failure in listOf(SessionExpiredException(), ApiResponseException(401, "Sessão inválida."))) {
                val source = BalancePreferencesFake()
                val model = BalancePreferencesViewModel(source)
                val count = events.size
                try {
                    model.load("synthetic"); runCurrent()
                    if (patch) {
                        source.writeFailure = failure
                        model.setExcluded("synthetic", BalanceBenefit.VA, true)
                    } else {
                        source.readFailure = failure
                        model.load("synthetic", force = true)
                    }
                    runCurrent()
                    assertNull(model.state.value.preferences)
                    assertFalse(model.state.value.canEdit)
                    assertTrue(model.state.value.loadFailed)
                    assertEquals(count + 1, events.size)
                    assertEquals("Sua sessão expirou. Faça login novamente.", events.last())
                } finally { model.close() }
            }
        } finally { collector.cancel() }
    } }

    @Test fun obsoleteReadAndPatchFailuresCannotRevokeTheNewSession() = runTest { withMain {
        val events = mutableListOf<String>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { SessionAccessEvents.accessRevoked.collect { events += it } }
        try {
            for (patch in listOf(false, true)) {
                val gate = CompletableDeferred<Unit>()
                val source = object : BalancePreferencesDataSource by BalancePreferencesFake() {
                    override suspend fun getBalancePreferences(token: String): BalancePreferences {
                        if (token == "old-synthetic" && !patch) {
                            withContext(NonCancellable) { gate.await() }
                            throw SessionExpiredException()
                        }
                        return BalancePreferences(false, false)
                    }
                    override suspend fun updateBalancePreferences(token: String, request: BalancePreferencesUpdate): BalancePreferences {
                        withContext(NonCancellable) { gate.await() }
                        throw SessionExpiredException()
                    }
                }
                val model = BalancePreferencesViewModel(source)
                try {
                    model.load("old-synthetic"); runCurrent()
                    if (patch) { model.setExcluded("old-synthetic", BalanceBenefit.VA, true); runCurrent() }
                    model.load("new-synthetic"); runCurrent()
                    gate.complete(Unit); runCurrent()
                    assertEquals(BalancePreferences(false, false), model.state.value.preferences)
                    assertTrue(model.state.value.canEdit)
                    assertNull(model.state.value.error)
                    assertTrue(events.isEmpty())
                } finally { gate.complete(Unit); model.close() }
            }
        } finally { collector.cancel() }
    } }

    @Test fun coroutineCancellationIsNeverPresentedAsFailureOrConfirmedSave() = runTest { withMain {
        val source = BalancePreferencesFake().apply { readFailure = CancellationException("synthetic") }
        val model = BalancePreferencesViewModel(source)
        val revision = FinanceDataChanges.revision.value
        try {
            model.load("synthetic"); runCurrent()
            assertNull(model.state.value.error)
            assertNull(model.state.value.preferences)
            assertFalse(model.state.value.loadFailed)
            source.readFailure = null
            model.load("synthetic", force = true); runCurrent()
            source.writeFailure = CancellationException("synthetic")
            model.setExcluded("synthetic", BalanceBenefit.VA, true); runCurrent()
            assertEquals(BalancePreferences(false, false), model.state.value.preferences)
            assertNull(model.state.value.error)
            assertNull(model.state.value.saving)
            assertEquals(revision, FinanceDataChanges.revision.value)
            assertEquals(1, source.writes.size)
        } finally { model.close() }
    } }
}
