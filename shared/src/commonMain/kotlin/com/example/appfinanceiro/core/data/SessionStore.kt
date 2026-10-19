package com.example.appfinanceiro.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

interface SessionStore {
    val token: Flow<String?>
    val userName: Flow<String>
    val userEmail: Flow<String>
    val userRole: Flow<String>
    val userAvatarUrl: Flow<String>
    val userAvatarCacheVersion: Flow<Long?>
    val biometricEnabledEmails: Flow<Set<String>>
    suspend fun saveToken(token: String, name: String, email: String, role: String, avatarUrl: String? = null)
    suspend fun saveAvatarUrl(avatarUrl: String?, cacheVersion: Long? = null)
    fun isBiometricEnabledForUser(email: String): Flow<Boolean>
    suspend fun setBiometricEnabledForUser(email: String, enabled: Boolean)
    suspend fun clearSession()
}

@Serializable
data class SessionData(
    val token: String? = null,
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val avatarUrl: String = "",
    val avatarCacheVersion: Long? = null,
    val biometricEnabledEmails: Set<String> = emptySet()
)

interface SessionPersistence {
    val data: Flow<SessionData>
    suspend fun update(transform: (SessionData) -> SessionData)
}

class PersistentSessionStore(private val persistence: SessionPersistence) : SessionStore {
    override val token = persistence.data.map { it.token }
    override val userName = persistence.data.map { it.name }
    override val userEmail = persistence.data.map { it.email }
    override val userRole = persistence.data.map { it.role }
    override val userAvatarUrl = persistence.data.map { it.avatarUrl }
    override val userAvatarCacheVersion = persistence.data.map { it.avatarCacheVersion }
    override val biometricEnabledEmails = persistence.data.map { it.biometricEnabledEmails }

    override suspend fun saveToken(token: String, name: String, email: String, role: String, avatarUrl: String?) {
        persistence.update { current ->
            current.copy(token = token, name = name, email = email, role = role,
                avatarUrl = avatarUrl?.takeIf(String::isNotBlank).orEmpty(),
                avatarCacheVersion = if (avatarUrl.isNullOrBlank()) null else current.avatarCacheVersion)
        }
    }
    override suspend fun saveAvatarUrl(avatarUrl: String?, cacheVersion: Long?) {
        persistence.update { current ->
            current.copy(avatarUrl = avatarUrl?.takeIf(String::isNotBlank).orEmpty(), avatarCacheVersion =
                if (avatarUrl.isNullOrBlank()) null else cacheVersion ?: current.avatarCacheVersion)
        }
    }
    override fun isBiometricEnabledForUser(email: String): Flow<Boolean> =
        biometricEnabledEmails.map { email.trim().lowercase() in it }

    override suspend fun setBiometricEnabledForUser(email: String, enabled: Boolean) {
        val normalizedEmail = email.trim().lowercase()
        persistence.update { current ->
            current.copy(biometricEnabledEmails = if (enabled) current.biometricEnabledEmails + normalizedEmail
                else current.biometricEnabledEmails - normalizedEmail)
        }
    }
    override suspend fun clearSession() {
        persistence.update { SessionData(biometricEnabledEmails = it.biometricEnabledEmails) }
    }
}
