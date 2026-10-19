package com.example.appfinanceiro.core.network

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun parseApiErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null
    return runCatching {
        val json = FinanceJson.parseToJsonElement(errorBody).jsonObject
        sequenceOf("error", "message", "detail").mapNotNull { key ->
            json[key]?.takeUnless { it === JsonNull }?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
        }.firstOrNull()
    }.getOrNull()
}
