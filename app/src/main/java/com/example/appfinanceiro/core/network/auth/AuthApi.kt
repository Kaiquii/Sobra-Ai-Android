package com.example.appfinanceiro.core.network.auth

import android.util.Log
import com.example.appfinanceiro.BuildConfig
import com.example.appfinanceiro.core.network.FinanceApi
import com.example.appfinanceiro.core.network.SessionAccessEvents
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)

data class ForgotPasswordRequest(val email: String)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val new_password: String
)

data class UserResponse(
    val name: String,
    val email: String,
    val role: String,
    val avatar_url: String? = null
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserResponse
)

data class RegisterResponse(val message: String, val user_id: Int)
data class DefaultAuthResponse(val message: String)

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): DefaultAuthResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): DefaultAuthResponse
}

object RetrofitClient {
    private val authenticatedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val startedAt = System.nanoTime()
                val response = chain.proceed(request)
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

                if (request.url.encodedPath.contains("installment-commitments")) {
                    Log.d(
                        "API_DEBUG",
                        "GET ${request.url} -> ${response.code} em ${elapsedMs}ms"
                    )
                }

                if (response.code == 403) {
                    val errorMessage = parseApiErrorMessage(
                        response.peekBody(2048).string()
                    )

                    if (errorMessage?.contains("Acesso revogado", ignoreCase = true) == true) {
                        SessionAccessEvents.notifyAccessRevoked(errorMessage)
                    }
                }

                response
            }
            .build()
    }

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    val financeApi: FinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinanceApi::class.java)
    }
}
