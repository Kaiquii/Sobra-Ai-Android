package com.example.appfinanceiro.core.network.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class RequestRegisterCodeRequest(
    val email: String,
    val protection_provider: String,
    val play_integrity_token: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val code: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
    val protection_provider: String,
    val play_integrity_token: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val new_password: String
)

@Serializable
data class UserResponse(
    val name: String,
    val email: String,
    val role: String,
    val avatar_url: String? = null
)

@Serializable
data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserResponse
)

@Serializable
data class RegisterResponse(val message: String, val user_id: Int)
@Serializable
data class DefaultAuthResponse(val message: String)

