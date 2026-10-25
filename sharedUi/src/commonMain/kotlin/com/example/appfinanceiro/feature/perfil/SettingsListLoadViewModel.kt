package com.example.appfinanceiro.feature.perfil

import br.com.sobraai.ui.runtime.PresentationModel
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

data class SettingsListLoadState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val error: String? = null
)

/** Keeps category/income reads and their visible state alive across shared navigation. */
class SettingsListLoadViewModel<T> : PresentationModel() {
    private val mutable = MutableStateFlow(SettingsListLoadState<T>())
    val state = mutable.asStateFlow()

    fun load(token: String, revision: Any?, fallback: String, read: suspend () -> List<T>) {
        launchLatest("settings", listOf(token, revision), reuseRecent = true) {
            mutable.value = mutable.value.copy(loading = true,
                error = if (mutable.value.items.isEmpty()) null else mutable.value.error)
            try {
                val items = read()
                currentCoroutineContext().ensureActive()
                mutable.value = SettingsListLoadState(items, loading = false, hasLoadedOnce = true)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: SessionExpiredException) {
                // FinanceActionsViewModel already forwards expiration to the session controller.
                currentCoroutineContext().ensureActive()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                mutable.value = mutable.value.copy(error = error.userMessageOr(fallback))
            } finally {
                if (currentCoroutineContext().isActive) {
                    mutable.value = mutable.value.copy(loading = false, hasLoadedOnce = true)
                }
            }
        }
    }
}
