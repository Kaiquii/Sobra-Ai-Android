package com.example.appfinanceiro.core.data

import kotlinx.io.IOException

open class ApiResponseException(
    val statusCode: Int,
    open val apiMessage: String?,
    errorCode: String? = null,
    retryAfterSeconds: Int? = null
) : Exception(apiMessage) {
    var errorCode: String? = errorCode
        internal set
    var retryAfterSeconds: Int? = retryAfterSeconds
        internal set
}
enum class NetworkFailureKind { Connection, Timeout }
class NetworkRequestException(val userMessage: String, cause: Throwable,
    val kind: NetworkFailureKind = NetworkFailureKind.Connection) : IOException(userMessage, cause)

fun Throwable.userMessageOr(defaultMessage: String): String = when (this) {
    is ApiResponseException -> apiMessage?.takeIf { it.isNotBlank() } ?: defaultMessage
    is NetworkRequestException -> userMessage
    else -> defaultMessage
}
