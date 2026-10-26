package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.ApiResponseException
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.NetworkFailureKind
import com.example.appfinanceiro.core.data.SessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString

class ApiConfiguration(baseUrl: String) {
    val baseUrl: String = baseUrl.trim().trimEnd('/') + "/"
    init {
        val parsed = Url(this.baseUrl)
        require(parsed.protocol == URLProtocol.HTTPS && parsed.host.isNotBlank()) { "A API deve usar HTTPS." }
        require(parsed.user == null && parsed.password == null && parsed.parameters.isEmpty() && parsed.fragment.isEmpty()) {
            "A URL base não pode conter credenciais, parâmetros ou fragmento."
        }
    }
}

expect fun createPlatformHttpClient(): HttpClient

internal fun HttpClientConfig<*>.configureFinanceHttpClient() {
    expectSuccess = false
    // Ktor segue GET/HEAD, recusa downgrade HTTPS e remove Bearer ao mudar de host.
    // Escritas não são reenviadas automaticamente por redirecionamento.
    followRedirects = true
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 90_000
    }
}

class ApiTransport(
    private val client: HttpClient,
    private val configuration: ApiConfiguration,
    private val apiErrorFactory: (Int, String?) -> Exception = ::ApiResponseException
) {
    suspend inline fun <reified T> request(
        method: String, path: String, authorization: String? = null,
        query: Map<String, Any?> = emptyMap(), body: String? = null,
        authenticated: Boolean = true, emptyResponse: String? = null
    ): T {
        val response = execute(method, path, authorization, query, body, authenticated)
        return FinanceJson.decodeFromString(if (response.isBlank() && emptyResponse != null) emptyResponse else response)
    }

    @PublishedApi
    internal suspend fun execute(
        method: String, path: String, authorization: String?, query: Map<String, Any?>,
        body: String?, authenticated: Boolean
    ): String {
        require(!path.startsWith('/') && !path.contains("://") && !path.contains("..")) { "Caminho de API inválido." }
        val connectionRetryDelays = longArrayOf(500, 1_500, 3_000)
        var retries = 0
        while (true) {
            try {
                return executeOnce(method, path, authorization, query, body, authenticated)
            } catch (error: NetworkRequestException) {
                // Dá tempo para a conexão transitória se recuperar, inclusive no retorno ao app.
                // Somente GET sem corpo repete; timeouts e escritas mantêm os limites originais.
                if (method != "GET" || body != null || error.kind != NetworkFailureKind.Connection ||
                    retries >= connectionRetryDelays.size) throw error
                delay(connectionRetryDelays[retries++])
            }
        }
    }

    private suspend fun executeOnce(
        method: String, path: String, authorization: String?, query: Map<String, Any?>,
        body: String?, authenticated: Boolean
    ): String {
        try {
            val response = client.request(configuration.baseUrl + path) {
                this.method = HttpMethod.parse(method)
                accept(ContentType.Application.Json)
                authorization?.let { header(HttpHeaders.Authorization, it) }
                query.forEach { (name, value) -> if (value != null) parameter(name, value.toString()) }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            val responseText = response.bodyAsText()
            val status = response.status.value
            if (status !in 200..299) {
                val message = parseApiErrorMessage(responseText)
                if (authenticated && status == 401) throw SessionExpiredException()
                if (authenticated && status == 403 && message?.contains("Acesso revogado", ignoreCase = true) == true) {
                    SessionAccessEvents.notifyAccessRevoked(message)
                }
                throw apiErrorFactory(status, message)
            }
            return responseText
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpRequestTimeoutException) {
            throw timeout(error)
        } catch (error: ConnectTimeoutException) {
            throw timeout(error)
        } catch (error: SocketTimeoutException) {
            throw timeout(error)
        } catch (error: IOException) {
            throw NetworkRequestException("Não foi possível conectar ao servidor. Verifique sua internet.", error)
        }
    }

    private fun timeout(cause: Throwable) = NetworkRequestException("O servidor demorou para responder. Tente novamente.", cause, NetworkFailureKind.Timeout)
}
