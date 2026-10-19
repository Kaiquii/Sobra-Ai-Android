package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.SharedFinanceApi
import com.example.appfinanceiro.core.network.ProfilePhotoResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import com.example.appfinanceiro.core.network.AndroidApiClient
import okhttp3.MultipartBody

class FinanceRepository(api: SharedFinanceApi = AndroidApiClient.financeApi) : SharedFinanceRepository(api) {
    suspend fun updateProfilePhoto(token: String, photo: MultipartBody.Part): ProfilePhotoResponse =
        executeApiRequest { RetrofitClient.financeApi.updateProfilePhoto("Bearer $token", photo) }
}