package com.example.appfinanceiro.feature.perfil

import br.com.sobraai.ui.runtime.PresentationModel
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileLoadState(
    val loading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val error: String? = null
)

/** Retains the profile request across tab changes, just like the financial models. */
class ProfileLoadViewModel(
    private val loadAndPersist: suspend (String) -> Unit,
    private val onExpired: () -> Unit
) : PresentationModel() {
    private val mutable = MutableStateFlow(ProfileLoadState())
    val state = mutable.asStateFlow()
    private var lastToken: String? = null

    fun load(token: String, hasCachedData: Boolean, retry: Boolean = false) {
        // Reopening a failed tab is not a user request to retry; keep the error stable.
        if (lastToken == token && mutable.value.error != null && !retry) return
        lastToken = token
        launchLatest("profile", token) {
            mutable.value = mutable.value.copy(loading = true,
                error = if (hasCachedData) mutable.value.error else null)
            try {
                loadAndPersist(token)
                currentCoroutineContext().ensureActive()
                mutable.value = ProfileLoadState(hasLoadedOnce = true)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                onExpired()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                mutable.value = ProfileLoadState(hasLoadedOnce = true,
                    error = error.userMessageOr("Não foi possível carregar o perfil."))
            }
        }
    }
}
