package com.example.appfinanceiro.core.network

import br.com.sobraai.app.BuildConfig
import com.example.appfinanceiro.core.data.ApiRequestException
import com.example.appfinanceiro.core.network.auth.KtorAuthApi

object AndroidApiClient {
    private val transport by lazy {
        ApiTransport(createPlatformHttpClient(), ApiConfiguration(BuildConfig.API_BASE_URL), ::ApiRequestException)
    }
    val patrimony by lazy { com.example.appfinanceiro.core.data.SharedPatrimonyRepository(transport) }
    val financeApi: SharedFinanceApi by lazy { KtorFinanceApi(transport) }
    val authApi by lazy { KtorAuthApi(transport) }
}
