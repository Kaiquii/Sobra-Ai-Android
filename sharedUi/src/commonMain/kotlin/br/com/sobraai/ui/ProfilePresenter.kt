package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePresenter(private val source: ProfileDataSource, private val files: FinanceFilesDataSource,
    private val native: NativeFeatures, private val session: SessionStore, private val token: String,
    private val scope: CoroutineScope, private val onExpired: () -> Unit, private val onFinancialChange: () -> Unit,
    private val biometricChange: suspend (Boolean) -> Unit, private val sessionChanged: suspend () -> Unit,
    val biometricKind: BiometricKind = BiometricKind.Generic) {
    val profile = FeatureQuery<Unit, ProfileUser>(Unit, scope, onExpired) { source.getProfile(token).user }
    val categories = FeatureQuery<Unit, List<Category>>(Unit, scope, onExpired) { source.getCategories(token).categories }
    val action = FeatureAction(scope, onExpired)
    private val mutableAvatar = MutableStateFlow<ByteArray?>(null)
    val avatar = mutableAvatar.asStateFlow()
    private val mutablePhotoError = MutableStateFlow<String?>(null)
    val photoError = mutablePhotoError.asStateFlow()
    val biometricEnabled = session.userEmail.flatMapLatest { session.isBiometricEnabledForUser(it) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    private var photoJob: Job? = null
    fun open() {
        profile.open()
    }
    fun loadPhoto(reference: String) {
        photoJob?.cancel()
        if (reference.isBlank()) { mutableAvatar.value = null; return }
        photoJob = scope.launch {
            try { val bytes = files.avatar(token, reference); ensureActive(); mutableAvatar.value = bytes; mutablePhotoError.value = null }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutablePhotoError.value = error.userMessageOr("Não foi possível carregar a foto.") }
        }
    }
    fun save(name: String, email: String) {
        if (name.isBlank() || !validEmail(email)) { action.error("Preencha nome e e-mail válido."); return }
        action.run {
            val previousEmail = session.userEmail.first()
            val enabled = session.isBiometricEnabledForUser(previousEmail).first()
            source.updateProfile(token, UpdateProfileRequest(name.trim(), email.trim()))
            currentCoroutineContext().ensureActive()
            val user = source.getProfile(token).user
            currentCoroutineContext().ensureActive()
            if (enabled && !previousEmail.equals(user.email, true)) session.setBiometricEnabledForUser(user.email, true)
            session.saveToken(token, user.name, user.email, user.role, user.avatar_url)
            sessionChanged(); profile.refresh()
            "Perfil atualizado."
        }
    }
    fun photo() { action.run {
        val selected = native.pickPhoto() ?: return@run null
        val response = files.uploadPhoto(token, selected)
        currentCoroutineContext().ensureActive()
        session.saveAvatarUrl(response.avatar_url, Clock.System.now().toEpochMilliseconds())
        photoJob?.cancel(); mutableAvatar.value = selected.bytes; mutablePhotoError.value = null
        profile.refresh(); "Foto atualizada."
    } }
    fun removePhoto() { action.run {
        source.deleteProfilePhoto(token); currentCoroutineContext().ensureActive(); session.saveAvatarUrl(null)
        photoJob?.cancel(); mutableAvatar.value = null; mutablePhotoError.value = null; profile.refresh(); "Foto removida."
    } }
    fun biometric(enabled: Boolean) { action.run { biometricChange(enabled); "Preferência de biometria atualizada." } }
    fun category(name: String, existing: Category? = null) {
        if (name.isBlank()) { action.error("Informe o nome da categoria."); return }
        action.run {
            if (existing == null) source.createCategory(token, CategoryRequest(name.trim()))
            else source.updateCategory(token, existing.id, CategoryRequest(name.trim()))
            currentCoroutineContext().ensureActive()
            categories.refresh(); onFinancialChange(); "Categoria salva."
        }
    }
    fun deleteCategory(category: Category) { action.run { source.deleteCategory(token, category.id); currentCoroutineContext().ensureActive(); categories.refresh(); onFinancialChange(); "Categoria excluída." } }
}

fun validEmail(value: String): Boolean = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(value.trim())
