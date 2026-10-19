package com.example.appfinanceiro.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin) {
    configureFinanceHttpClient()
    engine { configureSession { timeoutIntervalForRequest = 90.0 } }
}
