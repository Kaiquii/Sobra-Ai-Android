package com.example.appfinanceiro.core.security

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

typealias PlayIntegrityException = br.com.sobraai.ui.runtime.AuthProtectionException

object PlayIntegrityProtection {
    private const val cloudProjectNumber = 800491222292L
    private val providerMutex = Mutex()

    private var integrityManager: StandardIntegrityManager? = null
    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    fun initialize(context: Context) {
        if (integrityManager == null) {
            integrityManager = IntegrityManagerFactory.createStandard(context.applicationContext)
        }
    }

    suspend fun warmUp() {
        prepareProvider()
    }

    suspend fun newToken(path: String, email: String): Pair<String, String> {
        val normalizedEmail = normalizeEmail(email)
        val requestHash = requestHash(path = path, normalizedEmail = normalizedEmail)

        try {
            return normalizedEmail to requestToken(requestHash)
        } catch (error: StandardIntegrityException) {
            if (error.errorCode == StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID) {
                providerMutex.withLock { tokenProvider = null }
                return normalizedEmail to requestToken(requestHash)
            }
            throw PlayIntegrityException(error)
        } catch (error: PlayIntegrityException) {
            throw error
        } catch (error: Exception) {
            throw PlayIntegrityException(error)
        }
    }

    internal fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    internal fun requestHash(path: String, normalizedEmail: String): String {
        val payload = "POST\n$path\n$normalizedEmail"
        return MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private suspend fun requestToken(requestHash: String): String {
        val provider = prepareProvider()
        return provider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).awaitResult().token()
    }

    private suspend fun prepareProvider(): StandardIntegrityManager.StandardIntegrityTokenProvider =
        providerMutex.withLock {
            tokenProvider ?: run {
                val manager = requireNotNull(integrityManager) {
                    "Play Integrity must be initialized when the app starts."
                }
                manager.prepareIntegrityToken(
                    StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(cloudProjectNumber)
                        .build()
                ).awaitResult().also { tokenProvider = it }
            }
        }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}
