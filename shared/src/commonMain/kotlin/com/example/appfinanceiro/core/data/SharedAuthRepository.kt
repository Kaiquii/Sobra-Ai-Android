package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.auth.*

data class AuthProtectionProof(val normalizedEmail: String, val provider: String, val token: String)
fun interface AuthRequestProtection {
    suspend fun newProof(path: String, email: String): AuthProtectionProof
}

open class SharedAuthRepository(private val api: AuthApi, private val protection: AuthRequestProtection? = null) {
    suspend fun login(email: String, password: String): LoginResponse = api.login(LoginRequest(email, password))
    suspend fun register(name: String, email: String, password: String, code: String): RegisterResponse =
        api.register(RegisterRequest(name, email, password, code))
    suspend fun resetPassword(email: String, code: String, newPassword: String): DefaultAuthResponse =
        api.resetPassword(ResetPasswordRequest(email, code, newPassword))

    private suspend fun proof(path: String, email: String): AuthProtectionProof {
        val proof = checkNotNull(protection) { "Proteção de cadastro/recuperação não configurada nesta plataforma." }.newProof(path, email)
        // O contrato Apple precisa ser confirmado pelo backend antes de enviar outra prova.
        check(proof.provider == "play_integrity" && proof.token.isNotBlank()) { "Protocolo de proteção não suportado pelo contrato atual." }
        return proof
    }
    suspend fun requestRegisterCode(email: String): DefaultAuthResponse {
        val proof = proof("/api/auth/request-register-code", email)
        return api.requestRegisterCode(RequestRegisterCodeRequest(proof.normalizedEmail, proof.provider, proof.token))
    }
    suspend fun forgotPassword(email: String): DefaultAuthResponse {
        val proof = proof("/api/auth/forgot-password", email)
        return api.forgotPassword(ForgotPasswordRequest(proof.normalizedEmail, proof.provider, proof.token))
    }
}
