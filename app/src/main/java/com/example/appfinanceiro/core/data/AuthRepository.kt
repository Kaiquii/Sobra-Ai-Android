package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.AndroidApiClient
import com.example.appfinanceiro.core.security.PlayIntegrityProtection

class AuthRepository : SharedAuthRepository(
    AndroidApiClient.authApi,
    AuthRequestProtection { path, email ->
        val (normalizedEmail, token) = PlayIntegrityProtection.newToken(path, email)
        AuthProtectionProof(normalizedEmail, "play_integrity", token)
    }
)