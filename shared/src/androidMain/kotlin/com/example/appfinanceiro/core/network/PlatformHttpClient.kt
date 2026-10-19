package com.example.appfinanceiro.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    configureFinanceHttpClient()
    engine {
        config {
            retryOnConnectionFailure(false)
            writeTimeout(30, TimeUnit.SECONDS)
        }
    }
}
