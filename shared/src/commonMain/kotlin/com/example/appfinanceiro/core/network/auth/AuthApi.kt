package com.example.appfinanceiro.core.network.auth

interface AuthApi {
    suspend fun login(request: LoginRequest): LoginResponse

    suspend fun register(request: RegisterRequest): RegisterResponse

    suspend fun requestRegisterCode(request: RequestRegisterCodeRequest): DefaultAuthResponse

    suspend fun forgotPassword(request: ForgotPasswordRequest): DefaultAuthResponse

    suspend fun resetPassword(request: ResetPasswordRequest): DefaultAuthResponse
}

