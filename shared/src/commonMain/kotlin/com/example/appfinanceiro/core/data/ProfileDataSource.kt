package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.*

interface ProfileDataSource {
    suspend fun getProfile(token: String): ProfileResponse
    suspend fun updateProfile(token: String, request: UpdateProfileRequest): DefaultResponse
    suspend fun getCategories(token: String): CategoriesResponse
    suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse
    suspend fun updateCategory(token: String, id: Int, request: CategoryRequest): DefaultResponse
    suspend fun deleteCategory(token: String, id: Int): DefaultResponse
    suspend fun deleteProfilePhoto(token: String): DefaultResponse
}
