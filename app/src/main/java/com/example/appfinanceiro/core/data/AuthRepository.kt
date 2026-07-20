package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.auth.DefaultAuthResponse
import com.example.appfinanceiro.core.network.auth.ForgotPasswordRequest
import com.example.appfinanceiro.core.network.auth.LoginRequest
import com.example.appfinanceiro.core.network.auth.LoginResponse
import com.example.appfinanceiro.core.network.auth.RegisterRequest
import com.example.appfinanceiro.core.network.auth.RegisterResponse
import com.example.appfinanceiro.core.network.auth.RequestRegisterCodeRequest
import com.example.appfinanceiro.core.network.auth.ResetPasswordRequest
import com.example.appfinanceiro.core.network.auth.RetrofitClient

class AuthRepository {
    suspend fun login(email: String, password: String): LoginResponse = executeApiRequest(
        authenticated = false
    ) {
        RetrofitClient.authApi.login(LoginRequest(email, password))
    }

    suspend fun requestRegisterCode(email: String): DefaultAuthResponse = executeApiRequest(
        authenticated = false
    ) {
        RetrofitClient.authApi.requestRegisterCode(RequestRegisterCodeRequest(email))
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        code: String
    ): RegisterResponse = executeApiRequest(authenticated = false) {
        RetrofitClient.authApi.register(
            RegisterRequest(name = name, email = email, password = password, code = code)
        )
    }

    suspend fun forgotPassword(email: String): DefaultAuthResponse = executeApiRequest(
        authenticated = false
    ) {
        RetrofitClient.authApi.forgotPassword(ForgotPasswordRequest(email))
    }

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): DefaultAuthResponse = executeApiRequest(authenticated = false) {
        RetrofitClient.authApi.resetPassword(
            ResetPasswordRequest(email = email, code = code, new_password = newPassword)
        )
    }
}
