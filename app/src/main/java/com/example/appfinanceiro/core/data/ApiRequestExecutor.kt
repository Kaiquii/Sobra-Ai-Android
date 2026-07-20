package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.SessionAccessEvents
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class ApiRequestException(
    val statusCode: Int,
    val apiMessage: String?
) : Exception(apiMessage)

class NetworkRequestException(
    val userMessage: String,
    cause: Throwable
) : IOException(userMessage, cause)

suspend fun <T> executeApiRequest(
    authenticated: Boolean = true,
    block: suspend () -> T
): T {
    return try {
        block()
    } catch (error: HttpException) {
        val message = parseApiErrorMessage(error.response()?.errorBody()?.string())

        if (authenticated && error.code() == 401) {
            throw SessionExpiredException()
        }

        if (
            authenticated &&
            error.code() == 403 &&
            message?.contains("Acesso revogado", ignoreCase = true) == true
        ) {
            SessionAccessEvents.notifyAccessRevoked(message)
        }

        throw ApiRequestException(
            statusCode = error.code(),
            apiMessage = message
        )
    } catch (error: SocketTimeoutException) {
        throw NetworkRequestException(
            userMessage = "O servidor demorou para responder. Tente novamente.",
            cause = error
        )
    } catch (error: IOException) {
        throw NetworkRequestException(
            userMessage = "Não foi possível conectar ao servidor. Verifique sua internet.",
            cause = error
        )
    }
}

fun Throwable.userMessageOr(defaultMessage: String): String {
    return when (this) {
        is ApiRequestException -> apiMessage?.takeIf { it.isNotBlank() } ?: defaultMessage
        is NetworkRequestException -> userMessage
        else -> defaultMessage
    }
}
