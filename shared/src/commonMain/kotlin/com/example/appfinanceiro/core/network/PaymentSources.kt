package com.example.appfinanceiro.core.network

/** API values shared by forms, split payments and filters on both platforms. */
val PAYMENT_SOURCE_VALUES = listOf("Salário", "Adiantamento", "Renda Extra", "VA", "VR")

fun canonicalPaymentSource(value: String): String {
    val key = value.trim().lowercase()
        .replace("á", "a").replace("ã", "a").replace("ç", "c").replace("õ", "o")
        .replace("é", "e").replace("í", "i")
        .filter { it.isLetterOrDigit() }
    return when (key) {
        "salario" -> "Salário"
        "adiantamento" -> "Adiantamento"
        "rendaextra" -> "Renda Extra"
        "va", "valealimentacao" -> "VA"
        "vr", "valerefeicao" -> "VR"
        else -> value.trim()
    }
}

fun paymentSourceDisplayName(value: String): String = when (val source = canonicalPaymentSource(value)) {
    "VA" -> "Vale-alimentação (VA)"
    "VR" -> "Vale-refeição (VR)"
    else -> source
}

/** Never fall back to another month: monthly credits have no carry-over. */
fun List<Income>.incomeFor(source: String, month: Int, year: Int): Income? = firstOrNull {
    canonicalPaymentSource(it.source) == canonicalPaymentSource(source) && it.month == month && it.year == year
}
