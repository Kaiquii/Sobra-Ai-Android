package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LoadedValue<K, T>(val key: K, val value: T)
data class QueryState<K, T>(val key: K, val data: LoadedValue<K, T>? = null, val loading: Boolean = false, val error: String? = null)

class FeatureQuery<K, T>(initial: K, private val scope: CoroutineScope, private val onExpired: () -> Unit,
    private val load: suspend (K) -> T) {
    private val mutable = MutableStateFlow(QueryState<K, T>(initial))
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var revision = 0
    private var started = false
    fun select(key: K) { mutable.update { it.copy(key = key) }; refresh() }
    fun open() { if (!started) refresh() }
    fun refreshIfLoaded() { if (started) refresh() }
    fun refresh() {
        if (!scope.isActive) return
        started = true; job?.cancel()
        val version = ++revision
        val key = mutable.value.key
        mutable.update { it.copy(loading = true) }
        job = scope.launch {
            try {
                val value = load(key); ensureActive()
                if (version == revision) mutable.update { it.copy(data = LoadedValue(key, value), loading = false, error = null) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { if (version == revision) mutable.update { it.copy(loading = false, error = error.userMessageOr("Não foi possível carregar os dados.")) } }
        }
    }
}
data class FeatureActionState(val busy: Boolean = false, val error: String? = null, val message: String? = null)
class FeatureAction(private val scope: CoroutineScope, private val onExpired: () -> Unit) {
    private val mutable = MutableStateFlow(FeatureActionState())
    val state = mutable.asStateFlow()
    fun reset() { if (!mutable.value.busy) mutable.value = FeatureActionState() }
    fun error(message: String) { if (!mutable.value.busy) mutable.value = FeatureActionState(error = message) }
    fun run(operation: suspend () -> String?) {
        if (mutable.value.busy || !scope.isActive) return
        mutable.value = FeatureActionState(busy = true)
        scope.launch {
            try { val message = operation(); ensureActive(); mutable.value = FeatureActionState(message = message) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: SessionExpiredException) { onExpired() }
            catch (error: Exception) { mutable.value = FeatureActionState(error = error.userMessageOr("Não foi possível concluir a operação.")) }
        }
    }
}
