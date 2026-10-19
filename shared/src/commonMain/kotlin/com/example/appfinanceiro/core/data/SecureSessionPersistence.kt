package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.FinanceJson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

interface SecureSessionStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
    suspend fun remove()
}

interface BiometricPreferenceStorage {
    fun read(): Set<String>
    fun write(emails: Set<String>)
}

// Não emite uma sessão vazia provisória nem ignora falhas do armazenamento seguro.
class SecureSessionPersistence(
    private val secureStorage: SecureSessionStorage,
    private val biometricPreferences: BiometricPreferenceStorage
) : SessionPersistence {
    private val mutex = Mutex()
    private val current = MutableStateFlow<SessionData?>(null)
    private suspend fun load(): SessionData = current.value ?: run {
        val stored = secureStorage.read()?.let { FinanceJson.decodeFromString<SessionData>(it) } ?: SessionData()
        stored.copy(biometricEnabledEmails = biometricPreferences.read()).also { current.value = it }
    }
    override val data: Flow<SessionData> = flow {
        mutex.withLock { load() }
        emitAll(current.filterNotNull())
    }
    override suspend fun update(transform: (SessionData) -> SessionData) {
        mutex.withLock {
            val previous = load()
            val next = transform(previous)
            if (next.token == null) secureStorage.remove()
            else secureStorage.write(FinanceJson.encodeToString(next.copy(biometricEnabledEmails = emptySet())))
            if (next.biometricEnabledEmails != previous.biometricEnabledEmails) biometricPreferences.write(next.biometricEnabledEmails)
            current.value = next
        }
    }
}
