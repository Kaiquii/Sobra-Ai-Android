package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.SessionAccessEvents
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

// A propriedade local preserva os smart casts das telas Android sem editá-las.
class ApiRequestException(statusCode: Int, override val apiMessage: String?) : ApiResponseException(statusCode, apiMessage)

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
            cause = error,
            kind = NetworkFailureKind.Timeout
        )
    } catch (error: IOException) {
        throw NetworkRequestException(
            userMessage = "Não foi possível conectar ao servidor. Verifique sua internet.",
            cause = error
        )
    }
}
