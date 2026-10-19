package br.com.sobraai.ui

import kotlin.math.abs
import kotlin.math.roundToLong

fun formatCurrency(value: Double): String {
    if (!value.isFinite()) return "Valor indisponível"
    val cents = (abs(value) * 100).roundToLong()
    val integer = (cents / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    return "${if (value < 0) "-" else ""}R$ $integer,${(cents % 100).toString().padStart(2, '0')}"
}

fun formatDate(value: String): String {
    val date = value.take(10).split('-')
    return if (date.size == 3 && date[0].length == 4 && date[1].length == 2 && date[2].length == 2)
        "${date[2]}/${date[1]}/${date[0]}" else value.ifBlank { "Data não informada" }
}

expect fun currentPeriod(): Period
