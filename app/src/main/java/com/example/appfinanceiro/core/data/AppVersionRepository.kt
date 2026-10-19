package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.AppVersionResponse
import com.example.appfinanceiro.core.network.AndroidApiClient

class AppVersionRepository {
    suspend fun getAndroidVersion(): AppVersionResponse = AndroidApiClient.financeApi.getAppVersion(platform = "android")
}
