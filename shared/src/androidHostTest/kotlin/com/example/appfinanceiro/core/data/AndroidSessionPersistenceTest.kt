package com.example.appfinanceiro.core.data

import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.*

class AndroidSessionPersistenceTest {
    @Test
    fun readsExistingKeysAndLogoutPreservesPreferencesAndUnknownData() = runTest {
        val file = File.createTempFile("synthetic-session", ".preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val preferences = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            preferences.edit {
                it[stringPreferencesKey("auth_token")] = "synthetic-token"
                it[stringPreferencesKey("user_name")] = "Teste"
                it[stringPreferencesKey("user_email")] = "teste@example.invalid"
                it[stringPreferencesKey("user_role")] = "user"
                it[stringPreferencesKey("user_avatar_url")] = "https://example.invalid/avatar"
                it[longPreferencesKey("user_avatar_cache_version")] = 7L
                it[stringSetPreferencesKey("biometric_enabled_emails")] = setOf("teste@example.invalid")
                it[stringPreferencesKey("unrelated_preference")] = "preservar"
            }
            val session = PersistentSessionStore(AndroidSessionPersistence(preferences))
            assertEquals("synthetic-token", session.token.first())
            assertEquals("Teste", session.userName.first())
            assertEquals("teste@example.invalid", session.userEmail.first())
            assertEquals("user", session.userRole.first())
            assertEquals(7L, session.userAvatarCacheVersion.first())
            assertTrue(session.isBiometricEnabledForUser("TESTE@example.invalid").first())
            session.saveToken("synthetic-next", "Teste2", "teste2@example.invalid", "user")
            assertEquals("synthetic-next", preferences.data.first()[stringPreferencesKey("auth_token")])
            assertNull(preferences.data.first()[stringPreferencesKey("user_avatar_url")])
            session.clearSession()
            val stored = preferences.data.first()
            for (key in listOf("auth_token", "user_name", "user_email", "user_role", "user_avatar_url")) {
                assertNull(stored[stringPreferencesKey(key)], key)
            }
            assertNull(stored[longPreferencesKey("user_avatar_cache_version")])
            assertEquals(setOf("teste@example.invalid"), stored[stringSetPreferencesKey("biometric_enabled_emails")])
            assertEquals("preservar", stored[stringPreferencesKey("unrelated_preference")])
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            file.delete()
        }
    }
}
