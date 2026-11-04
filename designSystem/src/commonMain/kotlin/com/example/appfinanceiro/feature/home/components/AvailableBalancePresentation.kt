package com.example.appfinanceiro.feature.home.components

import com.example.appfinanceiro.core.network.SummaryResponse

/** Describes the server's available balance without recalculating any financial amount. */
fun availableBalanceSubtitle(summary: SummaryResponse?): String {
    val balanceLabel = if ((summary?.total_geral_disponivel ?: 0.0) < 0.0)
        "Atenção: saldo negativo no mês" else "Saldo livre no mês"
    val excludedBenefits = when {
        summary?.exclude_va_from_available == true && summary.exclude_vr_from_available -> "VA e VR"
        summary?.exclude_va_from_available == true -> "VA"
        summary?.exclude_vr_from_available == true -> "VR"
        else -> return balanceLabel
    }
    return "$balanceLabel · sem $excludedBenefits"
}
