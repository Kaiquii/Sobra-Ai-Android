package com.example.appfinanceiro.core.network

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.Streaming
import okhttp3.MultipartBody
import okhttp3.ResponseBody


interface FinanceApi {
    @Multipart
    @PATCH("api/users/profile/photo")
    suspend fun updateProfilePhoto(
        @Header("Authorization") token: String,
        @Part photo: MultipartBody.Part
    ): ProfilePhotoResponse

    @Streaming
    @GET("api/reports/export")
    suspend fun exportReport(
        @Header("Authorization") token: String,
        @Query("type") type: String,
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Query("format") format: String,
        @Query("compare_month") compareMonth: Int? = null,
        @Query("compare_year") compareYear: Int? = null,
        @Query("months") months: Int? = null,
        @Query("include_current_month_as_paid") includeCurrentMonthAsPaid: Boolean? = null
    ): Response<ResponseBody>
}
