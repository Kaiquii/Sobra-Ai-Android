package br.com.sobraai.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import br.com.sobraai.ui.runtime.collectAppNotifications
import com.example.appfinanceiro.core.designsystem.components.AppNotificationType
import com.example.appfinanceiro.core.designsystem.components.AppNotificationVisuals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppNotificationHostTest {
    @Test fun newActionReplacesOldMessageAndUserCanDismissIt() = runTest {
        val events = MutableSharedFlow<AppNotificationVisuals>(extraBufferCapacity = 4)
        val host = SnackbarHostState()
        backgroundScope.launch { host.collectAppNotifications(events) }
        runCurrent()
        events.emit(AppNotificationVisuals("Operação concluída.", AppNotificationType.Success))
        runCurrent()
        assertEquals(AppNotificationType.Success, (host.currentSnackbarData!!.visuals as AppNotificationVisuals).type)
        events.emit(AppNotificationVisuals("Falha sintética.", AppNotificationType.Error, SnackbarDuration.Long))
        runCurrent()
        val current = host.currentSnackbarData!!
        assertEquals("Falha sintética.", current.visuals.message)
        assertEquals(SnackbarDuration.Long, current.visuals.duration)
        assertTrue(current.visuals.withDismissAction)
        current.dismiss(); runCurrent()
        assertNull(host.currentSnackbarData)
    }
}
