package com.example.appfinanceiro.core.network

/** Provider-neutral errors, including historical responses already saved by older servers. */
fun isAssistantQuotaError(code: String?): Boolean = code in setOf(
    "assistant_quota_exceeded", "assistant_user_limit", "gemini_quota_exceeded"
)

fun isAssistantError(code: String?): Boolean = code?.startsWith("assistant_") == true ||
    code == "gemini_quota_exceeded"

fun assistantRetryDelaySeconds(code: String?, retryAfterSeconds: Int?): Int? =
    retryAfterSeconds?.takeIf { isAssistantError(code) && it > 0 }
