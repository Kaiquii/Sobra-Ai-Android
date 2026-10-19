package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class AuthContractTest {
    @Test
    fun authEndpointsKeepBodiesAndProtectionPaths() = runTest {
        val paths = mutableListOf<String>()
        val proofs = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            assertEquals(HttpMethod.Post, request.method)
            assertNull(request.headers[HttpHeaders.Authorization])
            val body = FinanceJson.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("teste@example.invalid", body.getValue("email").jsonPrimitive.content)
            val response = when (request.url.encodedPath) {
                "/api/auth/request-register-code", "/api/auth/forgot-password" -> {
                    assertEquals("play_integrity", body.getValue("protection_provider").jsonPrimitive.content)
                    assertEquals("synthetic-proof", body.getValue("play_integrity_token").jsonPrimitive.content)
                    """{"message":"ok"}"""
                }
                "/api/auth/register" -> {
                    assertEquals(setOf("name", "email", "password", "code"), body.keys)
                    """{"message":"ok","user_id":1}"""
                }
                "/api/auth/reset-password" -> {
                    assertEquals(setOf("email", "code", "new_password"), body.keys)
                    """{"message":"ok"}"""
                }
                else -> error("Endpoint inesperado")
            }
            respond(response)
        }) { configureFinanceHttpClient() }
        try {
            val api = KtorAuthApi(ApiTransport(client, ApiConfiguration("https://example.invalid/")))
            val repository = SharedAuthRepository(api, AuthRequestProtection { path, _ ->
                proofs += path
                AuthProtectionProof("teste@example.invalid", "play_integrity", "synthetic-proof")
            })
            repository.requestRegisterCode("TESTE@example.invalid")
            repository.forgotPassword("TESTE@example.invalid")
            repository.register("Teste", "teste@example.invalid", "synthetic-password", "000000")
            repository.resetPassword("teste@example.invalid", "000000", "synthetic-password")
            assertEquals(listOf("/api/auth/request-register-code", "/api/auth/forgot-password"), proofs)
            assertEquals(listOf("/api/auth/request-register-code", "/api/auth/forgot-password", "/api/auth/register", "/api/auth/reset-password"), paths)
            assertFailsWith<IllegalStateException> { SharedAuthRepository(api).requestRegisterCode("teste@example.invalid") }
            assertFailsWith<IllegalStateException> {
                SharedAuthRepository(api, AuthRequestProtection { _, email -> AuthProtectionProof(email, "app_attest", "synthetic-proof") })
                    .forgotPassword("teste@example.invalid")
            }
            assertEquals(4, paths.size)
        } finally { client.close() }
    }
}
