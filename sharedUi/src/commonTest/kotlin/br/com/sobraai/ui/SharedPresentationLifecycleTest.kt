package br.com.sobraai.ui

import br.com.sobraai.ui.runtime.PresentationStore
import com.example.appfinanceiro.feature.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SharedPresentationLifecycleTest {
    @Test fun lateMonthCannotOverwriteCurrentSummaryOrClearItsLoadingState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val old = CompletableDeferred<Unit>()
        val current = CompletableDeferred<Unit>()
        val source = EditingFake().apply {
            beforeRead = { month -> withContext(NonCancellable) { if (month == 10) old.await() else current.await() } }
        }
        val model = HomeViewModel(source)
        try {
            model.loadSummary("synthetic", 10, 2026); runCurrent()
            model.loadSummary("synthetic", 11, 2026); runCurrent()
            old.complete(Unit); runCurrent()
            assertNull(model.uiState.value.summaryData)
            assertTrue(model.uiState.value.isSummaryLoading)
            current.complete(Unit); advanceUntilIdle()
            assertEquals(11, model.uiState.value.summaryData?.month)
            assertFalse(model.uiState.value.isSummaryLoading)
        } finally { model.close(); Dispatchers.resetMain() }
    }

    @Test fun tabModelsAreReusedAndSessionClosureCancelsPendingReaders() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        val source = EditingFake().apply { beforeRead = { withContext(NonCancellable) { gate.await() } } }
        val store = PresentationStore()
        val model = store.get("Home/HomeViewModel") { HomeViewModel(source) }
        try {
            assertSame(model, store.get<HomeViewModel>("Home/HomeViewModel") { error("A aba não deve recriar o modelo.") })
            model.loadSummary("synthetic", 10, 2026); runCurrent()
            store.close(); gate.complete(Unit); advanceUntilIdle()
            assertNull(model.uiState.value.summaryData)
            assertFalse(model.viewModelScope.isActive)
        } finally { store.close(); Dispatchers.resetMain() }
    }
}
