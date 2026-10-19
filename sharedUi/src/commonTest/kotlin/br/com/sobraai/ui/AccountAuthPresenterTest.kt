package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.network.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

internal open class AccountApiFake : AuthApi {
    var registrations = mutableListOf<RegisterRequest>()
    var resets = mutableListOf<ResetPasswordRequest>()
    override suspend fun login(request: LoginRequest): LoginResponse = error("unused")
    override suspend fun requestRegisterCode(request: RequestRegisterCodeRequest): DefaultAuthResponse = error("Apple must not use Play Integrity")
    override suspend fun forgotPassword(request: ForgotPasswordRequest): DefaultAuthResponse = error("Apple must not use Play Integrity")
    override suspend fun register(request: RegisterRequest): RegisterResponse { registrations += request; return RegisterResponse("Conta criada.", 1) }
    override suspend fun resetPassword(request: ResetPasswordRequest): DefaultAuthResponse { resets += request; return DefaultAuthResponse("Senha redefinida.") }
}
internal open class DeliveryFake : AppleCodeDelivery {
    override val available = true
    override val unavailableReason: String? = null
    var requests = mutableListOf<String>()
    override suspend fun registration(email: String): String { requests += email; return "Confira seu e-mail." }
    override suspend fun recovery(email: String): String = registration(email)
}
@OptIn(ExperimentalCoroutinesApi::class)
class AccountAuthPresenterTest {
    @Test fun pendingAppleDeliveryNeverCallsProtectedAndroidEndpoints() = runTest {
        val api = AccountApiFake()
        val p = AccountAuthPresenter(AccountMode.Register, api, PendingAppleCodeDelivery(), backgroundScope, {})
        p.update { it.copy(name = "Pessoa Teste", email = "pessoa@example.invalid", password = "synthetic") }
        p.requestCode(); runCurrent()
        assertFalse(p.state.value.deliveryEnabled)
        assertFalse(p.state.value.confirmation)
        assertNotNull(p.state.value.error)
        assertTrue(api.registrations.isEmpty())
    }
    @Test fun registrationValidatesCodeAndPreservesPasswordExactly() = runTest {
        val api = AccountApiFake(); var success = ""
        val p = AccountAuthPresenter(AccountMode.Register, api, PendingAppleCodeDelivery(), backgroundScope, { success = it })
        p.update { it.copy(name = " Pessoa Teste ", email = " pessoa@example.invalid ", password = " synthetic password ") }
        p.useExistingCode(); p.update { it.copy(code = "123") }; p.submit(); runCurrent()
        assertTrue(api.registrations.isEmpty())
        p.update { it.copy(code = "000000") }; p.submit(); p.submit(); runCurrent()
        assertEquals(listOf(RegisterRequest("Pessoa Teste", "pessoa@example.invalid", " synthetic password ", "000000")), api.registrations)
        assertEquals("Conta criada.", success)
        assertEquals(AccountDraft(), p.state.value.draft)
    }
    @Test fun recoveryRequiresMatchingPasswordsAndUsesNewPasswordField() = runTest {
        val api = AccountApiFake()
        val p = AccountAuthPresenter(AccountMode.Recovery, api, PendingAppleCodeDelivery(), backgroundScope, {})
        p.update { it.copy(email = "pessoa@example.invalid") }; p.useExistingCode()
        p.update { it.copy(code = "000000", password = "synthetic", confirm = "different") }; p.submit(); runCurrent()
        assertTrue(api.resets.isEmpty())
        p.update { it.copy(confirm = "synthetic") }; p.submit(); runCurrent()
        assertEquals(ResetPasswordRequest("pessoa@example.invalid", "000000", "synthetic"), api.resets.single())
    }
    @Test fun rateLimitIsRememberedPerNormalizedEmail() = runTest {
        var time = 1000L; val delivery = object : DeliveryFake() {
            override suspend fun registration(email: String): String { requests += email; throw ApiResponseException(429, "Aguarde.") }
        }
        val p = AccountAuthPresenter(AccountMode.Register, AccountApiFake(), delivery, backgroundScope, {}, { time })
        p.update { it.copy(name = "Teste", email = "Pessoa@example.invalid", password = "synthetic") }
        p.requestCode(); runCurrent(); p.requestCode(); runCurrent()
        assertEquals(1, delivery.requests.size)
        p.update { it.copy(email = "other@example.invalid") }; p.requestCode(); runCurrent()
        assertEquals(2, delivery.requests.size)
        p.update { it.copy(email = " PESSOA@example.invalid ") }; p.requestCode(); runCurrent()
        assertEquals(2, delivery.requests.size)
        time += 300_001; p.requestCode(); runCurrent()
        assertEquals(3, delivery.requests.size)
    }
    @Test fun canceledRegistrationCannotNavigateOrExposeSuccess() = runTest {
        val pending = CompletableDeferred<Unit>(); var completed = false
        val api = object : AccountApiFake() { override suspend fun register(request: RegisterRequest): RegisterResponse {
            withContext(NonCancellable) { pending.await() }; return RegisterResponse("ok", 1)
        } }
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val p = AccountAuthPresenter(AccountMode.Register, api, PendingAppleCodeDelivery(), scope, { completed = true })
        p.update { it.copy(name = "Teste", email = "pessoa@example.invalid", password = "synthetic", code = "000000") }
        p.useExistingCode(); p.submit(); runCurrent(); scope.cancel(); pending.complete(Unit); runCurrent()
        assertFalse(completed)
    }
}
