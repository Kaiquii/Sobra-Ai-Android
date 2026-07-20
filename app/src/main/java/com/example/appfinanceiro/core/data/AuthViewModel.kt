package com.example.appfinanceiro.core.data

import androidx.lifecycle.ViewModel
import com.example.appfinanceiro.core.network.auth.DefaultAuthResponse
import com.example.appfinanceiro.core.network.auth.LoginResponse
import com.example.appfinanceiro.core.network.auth.RegisterResponse

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    suspend fun login(email: String, password: String): LoginResponse =
        repository.login(email, password)

    suspend fun requestRegisterCode(email: String): DefaultAuthResponse =
        repository.requestRegisterCode(email)

    suspend fun register(
        name: String,
        email: String,
        password: String,
        code: String
    ): RegisterResponse = repository.register(name, email, password, code)

    suspend fun forgotPassword(email: String): DefaultAuthResponse =
        repository.forgotPassword(email)

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ): DefaultAuthResponse = repository.resetPassword(email, code, newPassword)
}
