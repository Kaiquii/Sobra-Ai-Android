package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.AppVersionResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient

class AppVersionRepository {
    suspend fun getAndroidVersion(): AppVersionResponse = executeApiRequest(
        authenticated = false
    ) {
        RetrofitClient.financeApi.getAppVersion(platform = "android")
    }
}
