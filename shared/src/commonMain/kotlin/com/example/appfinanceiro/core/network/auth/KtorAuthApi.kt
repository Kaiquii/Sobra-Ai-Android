package com.example.appfinanceiro.core.network.auth

import com.example.appfinanceiro.core.network.ApiTransport
import com.example.appfinanceiro.core.network.FinanceJson
import kotlinx.serialization.encodeToString

class KtorAuthApi(private val transport: ApiTransport) : AuthApi {
    override suspend fun login(request: LoginRequest): LoginResponse =
        transport.request("POST", "api/auth/login", body = FinanceJson.encodeToString(request), authenticated = false)
    override suspend fun register(request: RegisterRequest): RegisterResponse =
        transport.request("POST", "api/auth/register", body = FinanceJson.encodeToString(request), authenticated = false)
    override suspend fun requestRegisterCode(request: RequestRegisterCodeRequest): DefaultAuthResponse =
        transport.request("POST", "api/auth/request-register-code", body = FinanceJson.encodeToString(request), authenticated = false)
    override suspend fun forgotPassword(request: ForgotPasswordRequest): DefaultAuthResponse =
        transport.request("POST", "api/auth/forgot-password", body = FinanceJson.encodeToString(request), authenticated = false)
    override suspend fun resetPassword(request: ResetPasswordRequest): DefaultAuthResponse =
        transport.request("POST", "api/auth/reset-password", body = FinanceJson.encodeToString(request), authenticated = false)
}
