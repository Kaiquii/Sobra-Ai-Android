package com.example.appfinanceiro.core.data

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException

class ApiRequestExecutorTest {
    @Test
    fun convertsUnauthorizedResponseIntoSessionExpired() {
        assertThrows(SessionExpiredException::class.java) {
            runTest {
                executeApiRequest<Unit> {
                    throw httpException(401, "{\"error\":\"Sessão expirada\"}")
                }
            }
        }
    }

    @Test
    fun preservesBackendMessageForApiErrors() {
        val error = assertThrows(ApiRequestException::class.java) {
            runTest {
                executeApiRequest<Unit> {
                    throw httpException(400, "{\"error\":\"Dados inválidos\"}")
                }
            }
        }

        assertEquals(400, error.statusCode)
        assertEquals("Dados inválidos", error.apiMessage)
    }

    @Test
    fun mapsTimeoutToFriendlyNetworkError() {
        val error = assertThrows(NetworkRequestException::class.java) {
            runTest {
                executeApiRequest<Unit> { throw SocketTimeoutException() }
            }
        }

        assertEquals("O servidor demorou para responder. Tente novamente.", error.userMessage)
    }

    private fun httpException(status: Int, json: String): HttpException {
        val body = json.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(status, body))
    }
}
