package com.example.appfinanceiro.feature.perfil

import br.com.sobraai.ui.runtime.PresentationModel
import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.data.BalancePreferencesDataSource
import com.example.appfinanceiro.core.data.FinanceDataChanges
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.BalancePreferences
import com.example.appfinanceiro.core.network.BalancePreferencesUpdate
import com.example.appfinanceiro.core.network.SessionAccessEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class BalanceBenefit { VA, VR }

data class BalancePreferencesState(
    val preferences: BalancePreferences? = null,
    val loading: Boolean = false,
    val saving: BalanceBenefit? = null,
    val error: String? = null,
    val loadFailed: Boolean = false
) {
    val canEdit: Boolean
        get() = preferences != null && !loading && saving == null && !loadFailed
}

/** Retains server-confirmed choices; the server remains responsible for financial totals. */
class BalancePreferencesViewModel(private val source: BalancePreferencesDataSource) : PresentationModel() {
    private val mutable = MutableStateFlow(BalancePreferencesState())
    val state = mutable.asStateFlow()
    private var token: String? = null
    private var sessionGeneration = 0L
    private var read: Job? = null
    private var mutation: Job? = null

    fun load(token: String?, force: Boolean = false) {
        val sessionToken = token?.takeIf { it.isNotBlank() }
        if (this.token != sessionToken) {
            sessionGeneration++
            read?.cancel()
            mutation?.cancel()
            this.token = sessionToken
            mutable.value = BalancePreferencesState()
        }
        if (sessionToken == null || mutation?.isActive == true || mutable.value.saving != null) return
        // Reentering a failed screen is not an explicit request to retry its GET.
        if (mutable.value.loadFailed && !force) return
        val generation = sessionGeneration
        read = launchLatest("balance-preferences", listOf(sessionToken, generation), reuseRecent = !force) {
            mutable.update { it.copy(loading = true, error = it.error.takeIf { _ -> it.preferences != null }) }
            try {
                val response = source.getBalancePreferences(sessionToken)
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) mutable.update {
                    it.copy(preferences = response, error = null, loadFailed = false)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) {
                    mutable.update {
                        it.copy(preferences = it.preferences.takeUnless { error.revokesAccess() },
                            error = error.preferenceMessage("Não foi possível carregar as preferências de saldo."),
                            loadFailed = true)
                    }
                    error.notifyExpiredSession()
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration) {
                    mutable.update { it.copy(loading = false) }
                }
            }
        }
    }

    fun setExcluded(token: String, benefit: BalanceBenefit, excluded: Boolean) {
        val current = mutable.value
        if (this.token != token || token.isBlank() || !current.canEdit ||
            read?.isActive == true || mutation?.isActive == true) return
        val preferences = current.preferences ?: return
        val confirmed = when (benefit) {
            BalanceBenefit.VA -> preferences.exclude_va_from_available
            BalanceBenefit.VR -> preferences.exclude_vr_from_available
        }
        if (confirmed == excluded) return
        val request = when (benefit) {
            BalanceBenefit.VA -> BalancePreferencesUpdate(exclude_va_from_available = excluded)
            BalanceBenefit.VR -> BalancePreferencesUpdate(exclude_vr_from_available = excluded)
        }
        val generation = sessionGeneration
        mutable.update { it.copy(saving = benefit, error = null) }
        mutation = viewModelScope.launch {
            try {
                val response = source.updateBalancePreferences(token, request)
                currentCoroutineContext().ensureActive()
                if (generation != sessionGeneration) return@launch
                mutable.update { it.copy(preferences = response, error = null) }
                FinanceDataChanges.notifyBalancePreferencesChanged()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation == sessionGeneration) {
                    mutable.update {
                        it.copy(preferences = it.preferences.takeUnless { error.revokesAccess() },
                            error = error.preferenceMessage("Não foi possível salvar as preferências de saldo."),
                            loadFailed = it.loadFailed || error.revokesAccess())
                    }
                    error.notifyExpiredSession()
                }
            } finally {
                if (currentCoroutineContext().isActive && generation == sessionGeneration) {
                    mutable.update { it.copy(saving = null) }
                }
            }
        }
    }
}

private fun Throwable.revokesAccess(): Boolean = this is SessionExpiredException ||
    (this is ApiResponseException && (statusCode == 401 || statusCode == 403))

private fun Throwable.preferenceMessage(fallback: String): String =
    if (this is SessionExpiredException) "Sua sessão expirou. Faça login novamente." else userMessageOr(fallback)

private fun Throwable.notifyExpiredSession() {
    if (this is SessionExpiredException || (this is ApiResponseException && statusCode == 401)) {
        SessionAccessEvents.notifyAccessRevoked("Sua sessão expirou. Faça login novamente.")
    }
}
