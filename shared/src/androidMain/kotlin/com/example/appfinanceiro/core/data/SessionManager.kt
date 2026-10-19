package com.example.appfinanceiro.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(context: Context) : SessionStore by PersistentSessionStore(AndroidSessionPersistence(context.applicationContext.dataStore))

internal class AndroidSessionPersistence(private val dataStore: DataStore<Preferences>) : SessionPersistence {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val nameKey = stringPreferencesKey("user_name")
    private val emailKey = stringPreferencesKey("user_email")
    private val roleKey = stringPreferencesKey("user_role")
    private val avatarKey = stringPreferencesKey("user_avatar_url")
    private val cacheKey = longPreferencesKey("user_avatar_cache_version")
    private val biometricKey = stringSetPreferencesKey("biometric_enabled_emails")

    private fun read(preferences: Preferences) = SessionData(
        token = preferences[tokenKey], name = preferences[nameKey].orEmpty(),
        email = preferences[emailKey].orEmpty(), role = preferences[roleKey].orEmpty(),
        avatarUrl = preferences[avatarKey].orEmpty(), avatarCacheVersion = preferences[cacheKey],
        biometricEnabledEmails = preferences[biometricKey].orEmpty()
    )
    override val data = dataStore.data.map(::read)

    override suspend fun update(transform: (SessionData) -> SessionData) {
        dataStore.edit { preferences ->
            val next = transform(read(preferences))
            if (next.token == null) {
                preferences.remove(tokenKey)
                preferences.remove(nameKey)
                preferences.remove(emailKey)
                preferences.remove(roleKey)
            } else {
                preferences[tokenKey] = next.token
                preferences[nameKey] = next.name
                preferences[emailKey] = next.email
                preferences[roleKey] = next.role
            }
            if (next.avatarUrl.isBlank()) preferences.remove(avatarKey) else preferences[avatarKey] = next.avatarUrl
            if (next.avatarCacheVersion == null) preferences.remove(cacheKey) else preferences[cacheKey] = next.avatarCacheVersion
            preferences[biometricKey] = next.biometricEnabledEmails
        }
    }
}
