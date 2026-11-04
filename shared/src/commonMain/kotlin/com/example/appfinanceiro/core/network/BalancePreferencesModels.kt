package com.example.appfinanceiro.core.network

import kotlinx.serialization.Serializable

@Serializable
data class BalancePreferences(
    val exclude_va_from_available: Boolean,
    val exclude_vr_from_available: Boolean
)

@Serializable
data class BalancePreferencesUpdate(
    val exclude_va_from_available: Boolean? = null,
    val exclude_vr_from_available: Boolean? = null
)
