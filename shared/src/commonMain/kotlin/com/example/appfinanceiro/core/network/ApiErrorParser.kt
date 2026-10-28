package com.example.appfinanceiro.core.network

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

data class ApiErrorDetails(val message: String?, val code: String?, val retryAfterSeconds: Int?)

fun parseApiErrorMessage(errorBody: String?): String? {
    return parseApiErrorDetails(errorBody).message
}

fun parseApiErrorDetails(errorBody: String?): ApiErrorDetails {
    val empty = ApiErrorDetails(null, null, null)
    if (errorBody.isNullOrBlank()) return empty
    return runCatching {
        val json = FinanceJson.parseToJsonElement(errorBody).jsonObject
        fun text(vararg keys: String): String? = keys.asSequence().mapNotNull { key ->
            (json[key] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        }.firstOrNull()
        ApiErrorDetails(text("error", "message", "detail"), text("error_code", "code"),
            (json["retry_after_seconds"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 })
    }.getOrDefault(empty)
}
