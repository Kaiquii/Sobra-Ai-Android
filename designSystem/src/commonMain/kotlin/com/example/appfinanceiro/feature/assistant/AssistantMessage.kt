package com.example.appfinanceiro.feature.assistant

data class AssistantMessage(
    val role: String,
    val content: String,
    val displayTime: String? = null,
    val errorCode: String? = null,
    val retryAfterSeconds: Int? = null
)

