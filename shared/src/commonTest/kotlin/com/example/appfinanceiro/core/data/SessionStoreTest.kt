package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.FinanceJson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.encodeToString
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreTest {
    private class SecureMemory(var value: String? = null) : SecureSessionStorage {
        var failRead = false
        var failWrite = false
        var failRemove = false
        override suspend fun read(): String? { check(!failRead) { "synthetic-read-failure" }; return value }
        override suspend fun write(value: String) { check(!failWrite) { "synthetic-write-failure" }; this.value = value }
        override suspend fun remove() { check(!failRemove) { "synthetic-remove-failure" }; value = null }
    }
    private class PreferencesMemory(var emails: Set<String> = emptySet()) : BiometricPreferenceStorage {
        override fun read() = emails
        override fun write(emails: Set<String>) { this.emails = emails }
    }
    private fun store(storage: SecureSessionStorage, preferences: BiometricPreferenceStorage = PreferencesMemory()) =
        PersistentSessionStore(SecureSessionPersistence(storage, preferences))

    @Test
    fun savesReloadsAndClearsIdentityWhilePreservingBiometricChoice() = runTest {
        val storage = SecureMemory()
        val preferences = PreferencesMemory()
        val session = store(storage, preferences)
        session.setBiometricEnabledForUser("  TESTE@example.invalid  ", true)
        session.saveToken("synthetic-token", "Teste", "teste@example.invalid", "user", "https://example.invalid/avatar")
        session.saveAvatarUrl("https://example.invalid/avatar2", 12L)
        val reloaded = store(storage, preferences)
        assertEquals("synthetic-token", reloaded.token.first())
        assertEquals("Teste", reloaded.userName.first())
        assertEquals("teste@example.invalid", reloaded.userEmail.first())
        assertEquals("user", reloaded.userRole.first())
        assertEquals("https://example.invalid/avatar2", reloaded.userAvatarUrl.first())
        assertEquals(12L, reloaded.userAvatarCacheVersion.first())
        assertTrue(reloaded.isBiometricEnabledForUser("TESTE@example.invalid").first())
        reloaded.clearSession()
        assertNull(storage.value)
        assertNull(reloaded.token.first())
        assertEquals("", reloaded.userName.first())
        assertEquals("", reloaded.userEmail.first())
        assertEquals("", reloaded.userRole.first())
        assertEquals("", reloaded.userAvatarUrl.first())
        assertNull(reloaded.userAvatarCacheVersion.first())
        assertTrue(store(storage, preferences).isBiometricEnabledForUser("teste@example.invalid").first())
    }

    @Test
    fun avatarCacheAndBlankValuesKeepAndroidSemantics() = runTest {
        val session = store(SecureMemory())
        session.saveToken("synthetic-token", "Teste", "teste@example.invalid", "user", "avatar")
        session.saveAvatarUrl("avatar2", 7)
        session.saveAvatarUrl("avatar3")
        assertEquals(7L, session.userAvatarCacheVersion.first())
        session.saveToken("synthetic-token2", "Teste", "teste@example.invalid", "user", "avatar4")
        assertEquals(7L, session.userAvatarCacheVersion.first())
        session.saveToken("synthetic-token3", "Teste", "teste@example.invalid", "user", " ")
        assertEquals("", session.userAvatarUrl.first())
        assertNull(session.userAvatarCacheVersion.first())
        session.saveAvatarUrl(null)
        assertEquals("", session.userAvatarUrl.first())
    }

    @Test
    fun biometricChoiceIsPerAccountAndDisablingDoesNotEraseOtherAccounts() = runTest {
        val session = store(SecureMemory())
        session.setBiometricEnabledForUser("a@example.invalid", true)
        session.setBiometricEnabledForUser("b@example.invalid", true)
        session.setBiometricEnabledForUser(" A@example.invalid ", false)
        assertEquals(setOf("b@example.invalid"), session.biometricEnabledEmails.first())
        session.clearSession()
        assertEquals(setOf("b@example.invalid"), session.biometricEnabledEmails.first())
    }

    @Test
    fun initialReadWaitsForStorageInsteadOfEmittingFalseLogout() = runTest {
        val ready = CompletableDeferred<Unit>()
        val storage = object : SecureSessionStorage {
            override suspend fun read(): String? {
                ready.await()
                return FinanceJson.encodeToString(SessionData(token = "synthetic-token"))
            }
            override suspend fun write(value: String) = Unit
            override suspend fun remove() = Unit
        }
        val session = store(storage)
        val value = async { session.token.first() }
        runCurrent()
        assertFalse(value.isCompleted)
        ready.complete(Unit)
        assertEquals("synthetic-token", value.await())
    }

    @Test
    fun readCorruptionAndWriteFailuresArePropagated() = runTest {
        val storage = SecureMemory()
        storage.failRead = true
        assertFailsWith<IllegalStateException> { store(storage).token.first() }
        storage.failRead = false
        val session = store(storage)
        assertNull(session.token.first())
        storage.failWrite = true
        assertFailsWith<IllegalStateException> { session.saveToken("synthetic-token", "Teste", "teste@example.invalid", "user") }
        assertNull(session.token.first())
        assertNull(storage.value)
        storage.failWrite = false
        session.saveToken("synthetic-token", "Teste", "teste@example.invalid", "user")
        storage.failRemove = true
        assertFailsWith<IllegalStateException> { session.clearSession() }
        assertEquals("synthetic-token", session.token.first())
        assertFails { store(SecureMemory("invalid-json")).token.first() }
    }

    @Test
    fun concurrentUpdatesDoNotLoseBiometricChoices() = runTest {
        val session = store(SecureMemory())
        val changes = (1..20).map { index -> async { session.setBiometricEnabledForUser("user$index@example.invalid", true) } }
        changes.forEach { it.await() }
        assertEquals(20, session.biometricEnabledEmails.first().size)
    }
}
