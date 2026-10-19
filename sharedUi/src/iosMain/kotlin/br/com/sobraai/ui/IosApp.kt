@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package br.com.sobraai.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.core.network.auth.KtorAuthApi
import kotlinx.coroutines.*
import platform.LocalAuthentication.*
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

/** The Swift host owns exactly one instance, including its Keychain store and HTTP client. */
class IosApp(apiBaseUrl: String, sessionService: String) {
    private val scope = MainScope()
    private val client = createPlatformHttpClient()
    private val configuration = ApiConfiguration(apiBaseUrl)
    private val transport = ApiTransport(client, configuration)
    private val authApi = KtorAuthApi(transport)
    private val auth = SharedAuthRepository(authApi)
    private val finance = SharedFinanceRepository(KtorFinanceApi(transport))
    private val session = IosSessionStore(sessionService)
    private val native = IosNativeFeatures()
    private val files = KtorFinanceFiles(client, configuration)
    private val controller = AppController(auth::login, session, AppleBiometrics(), scope)
    private fun expired() { controller.logout("Sua sessão expirou. Entre novamente.") }

    fun viewController(): UIViewController = ComposeUIViewController {
        SobraAiApp(controller, { token ->
            HomePresenter(finance, token, scope, currentPeriod()) {
                controller.logout("Sua sessão expirou. Entre novamente.")
            }
        }, { token, home -> FinanceWorkspace(home, finance, finance, token, scope,
            featureFactory = { featureScope, changed -> WorkspaceFeatures(
                ReportsPresenter(finance, files, native, token, featureScope, currentPeriod(), ::expired),
                AssistantPresenter(finance, native, token, featureScope, ::expired, changed),
                ProfilePresenter(finance, files, native, session, token, featureScope, ::expired, changed,
                    controller::setBiometricEnabled, controller::refreshIdentity), native)
            }, onExpired = ::expired) },
            { mode, accountScope -> AccountAuthPresenter(mode, authApi, PendingAppleCodeDelivery(), accountScope,
                { message -> controller.accountRoute(AppRoute.Login, message) }) })
    }.also { native.host = it }

    fun enterBackground() { native.cancelInteractions(); controller.lock() }
    fun enterForeground() { controller.enterForeground() }
    fun close() { native.cancelInteractions(); scope.cancel(); client.close() }
}

private class AppleBiometrics : BiometricAuthenticator {
    override fun isAvailable(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    override suspend fun authenticate(): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val context = LAContext()
            if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
                continuation.resume(false)
            } else {
                continuation.invokeOnCancellation { context.invalidate() }
                context.evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                    localizedReason = "Desbloqueie o SobraAi para acessar sua conta.") { accepted, _ ->
                    if (continuation.isActive) continuation.resume(accepted)
                }
            }
        }
    }
}
