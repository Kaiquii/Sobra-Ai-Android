package com.example.appfinanceiro.core.network.auth

import br.com.sobraai.app.BuildConfig
import com.example.appfinanceiro.core.network.FinanceApi
import com.example.appfinanceiro.core.network.createFinanceGson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val authenticatedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val financeApi: FinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create(createFinanceGson()))
            .build()
            .create(FinanceApi::class.java)
    }
}
