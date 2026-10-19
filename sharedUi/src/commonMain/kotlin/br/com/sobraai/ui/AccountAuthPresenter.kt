package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

interface AppleCodeDelivery {
    val available: Boolean
    val unavailableReason: String?
    suspend fun registration(email: String): String
    suspend fun recovery(email: String): String
}
/** Enabled only after the backend owner supplies and implements an Apple attestation contract. */
class PendingAppleCodeDelivery : AppleCodeDelivery {
    override val available = false
    override val unavailableReason = "O envio de códigos no iOS ainda não foi habilitado pelo servidor."
    override suspend fun registration(email: String): String = error(unavailableReason)
    override suspend fun recovery(email: String): String = error(unavailableReason)
}
enum class AccountMode { Register, Recovery }
data class AccountDraft(val name: String = "", val email: String = "", val password: String = "", val confirm: String = "", val code: String = "")
data class AccountState(val draft: AccountDraft = AccountDraft(), val confirmation: Boolean = false, val busy: Boolean = false,
    val error: String? = null, val message: String? = null, val retryAt: Long = 0, val deliveryEnabled: Boolean = false, val deliveryReason: String? = null)
class AccountAuthPresenter(val mode: AccountMode, private val api: AuthApi, private val delivery: AppleCodeDelivery,
    private val scope: CoroutineScope, private val completed: (String) -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {
    private val mutable = MutableStateFlow(AccountState(deliveryEnabled = delivery.available, deliveryReason = delivery.unavailableReason))
    val state = mutable.asStateFlow()
    private val blocks = mutableMapOf<String, Long>()
    fun update(transform: (AccountDraft) -> AccountDraft) { if (!mutable.value.busy) mutable.update {
        val draft = transform(it.draft)
        it.copy(draft = draft, error = null, retryAt = blocks[draft.email.trim().lowercase()] ?: 0)
    } }
    fun details() { if (!mutable.value.busy) mutable.update { it.copy(confirmation = false, draft = it.draft.copy(code = ""), error = null) } }
    fun useExistingCode() {
        val d = mutable.value.draft
        if (mutable.value.busy) return
        if (!validEmail(d.email) || mode == AccountMode.Register && (d.name.isBlank() || d.password.isBlank())) {
            mutable.update { it.copy(error = "Preencha os dados antes de informar o código.") }; return
        }
        mutable.update { it.copy(confirmation = true, error = null) }
    }
    fun requestCode() {
        val state = mutable.value; if (state.busy) return
        val draft = state.draft
        val email = draft.email.trim().lowercase()
        val error = when {
            !validEmail(email) -> "Informe um e-mail válido."
            mode == AccountMode.Register && draft.name.isBlank() -> "Informe seu nome."
            mode == AccountMode.Register && draft.password.isBlank() -> "Crie uma senha."
            !delivery.available -> delivery.unavailableReason ?: "Envio de códigos indisponível."
            else -> null
        }
        if (error != null) { mutable.update { it.copy(error = error) }; return }
        if (now() < (blocks[email] ?: 0)) return
        mutable.update { it.copy(busy = true, error = null) }
        scope.launch {
            try {
                val message = if (mode == AccountMode.Register) delivery.registration(email) else delivery.recovery(email)
                ensureActive(); val until = now() + 60_000; blocks[email] = until
                mutable.update { it.copy(busy = false, confirmation = true, retryAt = until, message = message, draft = it.draft.copy(email = email, code = "")) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                val until = if (error is ApiResponseException && error.statusCode == 429) now() + 300_000 else 0
                if (until > 0) blocks[email] = until
                mutable.update { it.copy(busy = false, retryAt = until, error = error.userMessageOr("Não foi possível enviar o código.")) }
            }
        }
    }
    fun submit() {
        val state = mutable.value; if (state.busy || !state.confirmation) return
        val d = state.draft
        val error = when {
            !Regex("[0-9]{6}").matches(d.code) -> "O código deve ter 6 dígitos."
            d.password.isBlank() -> "Informe uma senha."
            mode == AccountMode.Recovery && d.password != d.confirm -> "As senhas não coincidem."
            else -> null
        }
        if (error != null) { mutable.update { it.copy(error = error) }; return }
        mutable.update { it.copy(busy = true, error = null) }
        scope.launch {
            try {
                val message = if (mode == AccountMode.Register) api.register(RegisterRequest(d.name.trim(), d.email.trim(), d.password, d.code)).message
                    else api.resetPassword(ResetPasswordRequest(d.email.trim(), d.code, d.password)).message
                ensureActive(); mutable.update { it.copy(busy = false, draft = AccountDraft()) }; completed(message)
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { mutable.update { it.copy(busy = false, error = error.userMessageOr("Código inválido ou expirado.")) } }
        }
    }
}
