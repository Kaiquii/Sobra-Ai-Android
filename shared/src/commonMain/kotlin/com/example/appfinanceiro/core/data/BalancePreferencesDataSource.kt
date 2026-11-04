package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.BalancePreferences
import com.example.appfinanceiro.core.network.BalancePreferencesUpdate

interface BalancePreferencesDataSource {
    suspend fun getBalancePreferences(token: String): BalancePreferences
    suspend fun updateBalancePreferences(
        token: String,
        request: BalancePreferencesUpdate
    ): BalancePreferences
}
