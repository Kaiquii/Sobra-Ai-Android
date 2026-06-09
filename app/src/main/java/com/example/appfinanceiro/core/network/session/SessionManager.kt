package com.example.appfinanceiro.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_AVATAR_URL_KEY = stringPreferencesKey("user_avatar_url")
        private val BIOMETRIC_ENABLED_EMAILS_KEY =
            stringSetPreferencesKey("biometric_enabled_emails")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME_KEY] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL_KEY] ?: "" }
    val userRole: Flow<String> = context.dataStore.data.map { it[USER_ROLE_KEY] ?: "" }
    val userAvatarUrl: Flow<String> = context.dataStore.data.map { it[USER_AVATAR_URL_KEY] ?: "" }

    val biometricEnabledEmails: Flow<Set<String>> =
        context.dataStore.data.map { it[BIOMETRIC_ENABLED_EMAILS_KEY] ?: emptySet() }

    suspend fun saveToken(
        token: String,
        name: String,
        email: String,
        role: String,
        avatarUrl: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_NAME_KEY] = name
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ROLE_KEY] = role

            if (avatarUrl.isNullOrBlank()) {
                preferences.remove(USER_AVATAR_URL_KEY)
            } else {
                preferences[USER_AVATAR_URL_KEY] = avatarUrl
            }
        }
    }

    suspend fun saveAvatarUrl(avatarUrl: String?) {
        context.dataStore.edit { preferences ->
            if (avatarUrl.isNullOrBlank()) {
                preferences.remove(USER_AVATAR_URL_KEY)
            } else {
                preferences[USER_AVATAR_URL_KEY] = avatarUrl
            }
        }
    }

    fun isBiometricEnabledForUser(email: String): Flow<Boolean> {
        return biometricEnabledEmails.map { enabledEmails ->
            enabledEmails.contains(email.trim().lowercase())
        }
    }

    suspend fun setBiometricEnabledForUser(email: String, enabled: Boolean) {
        val normalizedEmail = email.trim().lowercase()

        context.dataStore.edit { preferences ->
            val current = preferences[BIOMETRIC_ENABLED_EMAILS_KEY] ?: emptySet()

            preferences[BIOMETRIC_ENABLED_EMAILS_KEY] =
                if (enabled) {
                    current + normalizedEmail
                } else {
                    current - normalizedEmail
                }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_NAME_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USER_ROLE_KEY)
            preferences.remove(USER_AVATAR_URL_KEY)
        }
    }
}
