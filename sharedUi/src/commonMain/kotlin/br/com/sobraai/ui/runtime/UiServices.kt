package br.com.sobraai.ui.runtime

import androidx.compose.runtime.*
import br.com.sobraai.ui.*
import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.relatorios.export.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/** Native capabilities are injected once by the host; all routes and presentation stay common. */
class UiServices(
    val session: SessionStore,
    val finance: SharedFinanceRepository,
    val auth: SharedAuthRepository,
    val files: FinanceFilesDataSource,
    val native: NativeFeatures,
    val controller: AppController,
    val reports: ReportFiles,
    val apiBaseUrl: String,
    val codeDeliveryAvailable: Boolean = true,
    val installationHint: String = "Atualize ou reinstale o aplicativo pela loja oficial.",
    val updates: com.example.appfinanceiro.core.update.AppUpdates? = null
) {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    fun notify(message: String) { messages.tryEmit(message) }
    suspend fun persistProfileIdentity(token: String, name: String, email: String, role: String, avatarUrl: String?) {
        val previousEmail = session.userEmail.first()
        if (session.isBiometricEnabledForUser(previousEmail).first()) session.setBiometricEnabledForUser(email, true)
        currentCoroutineContext().ensureActive()
        session.saveToken(token, name, email, role, avatarUrl)
        controller.refreshIdentity()
    }
}

interface ReportFiles {
    suspend fun export(token: String, request: ReportExportRequest): ExportedReport?
    fun open(report: ExportedReport)
    fun share(report: ExportedReport)
}

class AuthProtectionException(cause: Throwable) : Exception(cause)

val LocalUiServices = staticCompositionLocalOf<UiServices> { error("O host precisa fornecer UiServices.") }
internal val LocalPresentationStore = staticCompositionLocalOf<PresentationStore?> { null }
internal val LocalPresentationRoute = staticCompositionLocalOf { "" }

/** Keeps tab models alive while the shared navigation changes; closing the session cancels all. */
internal class PresentationStore {
    private val models = mutableMapOf<String, PresentationModel>()
    @Suppress("UNCHECKED_CAST")
    fun <T : PresentationModel> get(key: String, factory: () -> T): T = models.getOrPut(key, factory) as T
    fun close() { models.values.forEach(PresentationModel::close); models.clear() }
}

open class PresentationModel {
    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private class Reader(val request: Any?, val job: Job)
    private val readers = mutableMapOf<String, Reader>()
    protected fun launchLatest(key: String, request: Any? = null, block: suspend CoroutineScope.() -> Unit): Job {
        val previous = readers[key]
        if (request != null && previous?.request == request && previous.job.isActive) return previous.job
        readers.remove(key)?.job?.cancel()
        return viewModelScope.launch(block = block).also { readers[key] = Reader(request, it) }
    }
    protected open fun onCleared() {}
    fun close() { viewModelScope.cancel(); onCleared() }
}

@Composable
fun <T : PresentationModel> rememberPresentation(key: String, factory: (UiServices) -> T): T {
    val services = LocalUiServices.current
    val store = LocalPresentationStore.current
    val route = LocalPresentationRoute.current
    val model = remember(services, store, route, key) {
        store?.get("$route/$key") { factory(services) } ?: factory(services)
    }
    DisposableEffect(model, store) { onDispose { if (store == null) model.close() } }
    return model
}

/** Screen messages have one common host, including on iPhone. */
object UiToast {
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1
    fun makeText(context: UiServices, message: String, duration: Int): Message = Message(context, message)
    class Message(private val services: UiServices, private val text: String) {
        fun show() = services.notify(text)
    }
}
