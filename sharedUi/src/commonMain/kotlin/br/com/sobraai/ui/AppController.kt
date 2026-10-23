package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.SessionAccessEvents
import com.example.appfinanceiro.core.network.auth.LoginResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

interface BiometricAuthenticator {
    fun isAvailable(): Boolean
    fun kind(): BiometricKind = BiometricKind.Generic
    suspend fun authenticate(): Boolean
}

enum class BiometricKind(val label: String) {
    FaceId("Face ID"), TouchId("Touch ID"), Generic("biometria")
}

enum class AppRoute { Loading, Login, Register, Recovery, BiometricOffer, Locked, Home, StorageError }
data class AppState(
    val route: AppRoute = AppRoute.Loading,
    val busy: Boolean = false,
    val message: String? = null,
    val blockedEmail: String = "",
    val blockedUntil: Long = 0
)

/** Scoped to one host instance; credentials never enter a saveable Compose state. */
class AppController(
    private val loginRequest: suspend (String, String) -> LoginResponse,
    private val session: SessionStore,
    private val biometrics: BiometricAuthenticator,
    private val scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    val biometricKind: BiometricKind get() = biometrics.kind()
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    private var token: String? = null
    private var email = ""
    private var biometricEnabled = false
    private var pending: LoginResponse? = null
    private var operation: Job? = null
    private var generation = 0
    private var background = false
    private var foreground = false
    private var automaticUnlockPending = false
    private var logoutPending = false
    var home: HomePresenter? = null
        private set
    var workspace: FinanceWorkspace? = null
        private set

    init {
        scope.launch { SessionAccessEvents.accessRevoked.collect { logout(it) } }
        restore()
    }

    fun restore() {
        if (logoutPending) { logout(); return }
        generation++
        operation?.cancel()
        automaticUnlockPending = false
        mutable.value = AppState()
        operation = scope.launch(start = CoroutineStart.LAZY) {
            try {
                token = session.token.first()?.takeIf { it.isNotBlank() }
                email = session.userEmail.first()
                biometricEnabled = session.isBiometricEnabledForUser(email).first()
                mutable.value = AppState(if (token == null) AppRoute.Login else if (biometricEnabled) AppRoute.Locked else AppRoute.Home)
                automaticUnlockPending = mutable.value.route == AppRoute.Locked
                unlockAutomaticallyIfReady()
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { mutable.value = AppState(AppRoute.StorageError, message = "Não foi possível acessar a sessão segura. Desbloqueie o aparelho e tente novamente.") }
        }
        operation?.start()
    }

    fun attachHome(factory: (String) -> HomePresenter): HomePresenter? {
        if (mutable.value.route != AppRoute.Home) return null
        return home ?: token?.let(factory)?.also { home = it; it.refresh() }
    }

    fun attachWorkspace(factory: (String, HomePresenter) -> FinanceWorkspace): FinanceWorkspace? {
        if (mutable.value.route != AppRoute.Home) return null
        return workspace ?: token?.let { value -> home?.let { factory(value, it) } }?.also { workspace = it }
    }

    fun accountRoute(route: AppRoute, message: String? = null) {
        require(route in listOf(AppRoute.Login, AppRoute.Register, AppRoute.Recovery))
        if (!mutable.value.busy && mutable.value.route in listOf(AppRoute.Login, AppRoute.Register, AppRoute.Recovery)) mutable.value = AppState(route, message = message)
    }
    fun clearLoginMessage() {
        if (mutable.value.route == AppRoute.Login && !mutable.value.busy) mutable.value = mutable.value.copy(message = null)
    }
    suspend fun refreshIdentity() {
        require(mutable.value.route == AppRoute.Home)
        email = session.userEmail.first()
        biometricEnabled = session.isBiometricEnabledForUser(email).first()
    }
    suspend fun setBiometricEnabled(enabled: Boolean) {
        require(mutable.value.route == AppRoute.Home && token != null)
        val version = generation
        if (enabled) {
            check(biometrics.isAvailable()) { "${biometricKind.label.replaceFirstChar { it.uppercase() }} não está disponível neste aparelho." }
            check(biometrics.authenticate()) { "A biometria não foi confirmada. A preferência foi mantida." }
        }
        currentCoroutineContext().ensureActive()
        check(version == generation && mutable.value.route == AppRoute.Home && token != null) { "A sessão foi alterada. Entre novamente." }
        email = session.userEmail.first()
        session.setBiometricEnabledForUser(email, enabled)
        biometricEnabled = enabled
        if (enabled && background) lock()
    }

    fun login(emailInput: String, password: String) {
        if (mutable.value.busy || mutable.value.route != AppRoute.Login) return
        val normalized = emailInput.trim().lowercase()
        if (normalized == mutable.value.blockedEmail && now() < mutable.value.blockedUntil) return
        if (emailInput.isBlank() || password.isBlank()) {
            mutable.value = mutable.value.copy(message = "Preencha e-mail e senha.")
            return
        }
        val version = ++generation
        mutable.value = mutable.value.copy(busy = true, message = null)
        operation = scope.launch {
            try {
                val result = loginRequest(emailInput.trim(), password)
                require(result.token.isNotBlank()) { "Resposta de sessão inválida." }
                if (version != generation) return@launch
                pending = result
                if (biometrics.isAvailable()) mutable.value = AppState(AppRoute.BiometricOffer)
                else persistLogin(false)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                if (version == generation) {
                    if (error is ApiResponseException && error.statusCode == 403) {
                        try { session.clearSession() }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) {
                            logoutPending = true
                            mutable.value = AppState(AppRoute.StorageError, message = "Não foi possível encerrar a sessão segura. Tente novamente.")
                            return@launch
                        }
                    }
                    mutable.value = mutable.value.copy(busy = false,
                        message = error.userMessageOr("Credenciais inválidas ou erro no servidor."),
                        blockedEmail = if (error is ApiResponseException && error.statusCode == 429) normalized else mutable.value.blockedEmail,
                        blockedUntil = if (error is ApiResponseException && error.statusCode == 429) now() + 300_000 else mutable.value.blockedUntil)
                }
            }
        }
    }

    fun finishLogin(enableBiometrics: Boolean) {
        if (mutable.value.busy || mutable.value.route != AppRoute.BiometricOffer) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        operation = scope.launch {
            try {
                if (enableBiometrics && !biometrics.authenticate()) {
                    mutable.value = mutable.value.copy(busy = false, message = "Não foi possível confirmar a biometria. Tente novamente ou escolha Agora não.")
                    return@launch
                }
                persistLogin(enableBiometrics)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { mutable.value = mutable.value.copy(busy = false, message = "Não foi possível salvar a sessão segura. Tente novamente.") }
        }
    }

    private suspend fun persistLogin(enabled: Boolean) {
        val result = pending ?: return
        session.setBiometricEnabledForUser(result.user.email, enabled)
        session.saveToken(result.token, result.user.name, result.user.email, result.user.role, result.user.avatar_url)
        token = result.token
        email = result.user.email
        biometricEnabled = enabled
        pending = null
        mutable.value = AppState(if (enabled && background) AppRoute.Locked else AppRoute.Home)
        automaticUnlockPending = mutable.value.route == AppRoute.Locked
    }

    fun unlock() {
        if (mutable.value.busy || mutable.value.route != AppRoute.Locked) return
        automaticUnlockPending = false
        val version = generation
        mutable.value = mutable.value.copy(busy = true, message = null)
        operation = scope.launch {
            try {
                val accepted = biometrics.authenticate()
                currentCoroutineContext().ensureActive()
                if (version != generation || mutable.value.route != AppRoute.Locked) return@launch
                mutable.value = AppState(if (accepted && !background) AppRoute.Home else AppRoute.Locked,
                    message = if (accepted) null else "Autenticação cancelada ou indisponível. Tente novamente ou entre com sua senha.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                if (version == generation && mutable.value.route == AppRoute.Locked) {
                    mutable.value = AppState(AppRoute.Locked, message = "Não foi possível autenticar com biometria.")
                }
            }
        }
    }

    fun lock() {
        background = true
        foreground = false
        // Hosts call this only for real background, never for the biometric sheet's inactive scene.
        if (biometricEnabled && mutable.value.route == AppRoute.Home) {
            workspace?.close(); workspace = null
            home?.clear(); home = null
            mutable.value = AppState(AppRoute.Locked)
        }
        if (mutable.value.route == AppRoute.Locked) {
            if (mutable.value.busy) {
                generation++
                operation?.cancel()
                mutable.value = AppState(AppRoute.Locked)
            }
            automaticUnlockPending = true
        }
    }

    fun enterForeground() {
        background = false
        foreground = true
        unlockAutomaticallyIfReady()
    }

    private fun unlockAutomaticallyIfReady() {
        if (foreground && !background && automaticUnlockPending &&
            mutable.value.route == AppRoute.Locked && !mutable.value.busy) {
            unlock()
        }
    }

    fun logout(message: String? = null) {
        logoutPending = true
        generation++
        automaticUnlockPending = false
        operation?.cancel()
        workspace?.close(); workspace = null
        home?.clear(); home = null; pending = null; token = null
        mutable.value = AppState(AppRoute.Loading)
        operation = scope.launch {
            try { session.clearSession(); logoutPending = false; mutable.value = AppState(AppRoute.Login, message = message) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { mutable.value = AppState(AppRoute.StorageError, message = "Não foi possível encerrar a sessão segura. Tente novamente.") }
        }
    }
}
