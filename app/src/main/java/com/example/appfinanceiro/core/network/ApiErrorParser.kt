package com.example.appfinanceiro.core.network

import com.google.gson.Gson
import com.google.gson.JsonObject

fun parseApiErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null

    return runCatching {
        val json: JsonObject = Gson().fromJson(errorBody, JsonObject::class.java)
        sequenceOf("error", "message", "detail")
            .mapNotNull { key ->
                json.get(key)
                    ?.takeUnless { it.isJsonNull }
                    ?.asString
                    ?.takeIf { it.isNotBlank() }
            }
            .firstOrNull()
    }.getOrNull()
}
