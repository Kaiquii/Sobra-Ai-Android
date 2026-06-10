package com.example.appfinanceiro.core.network

import org.json.JSONObject

fun parseApiErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null

    return runCatching {
        JSONObject(errorBody).optString("error").takeIf { it.isNotBlank() }
    }.getOrNull()
}
