package com.example.appfinanceiro.feature.relatorios.utils

import kotlin.math.abs
import kotlin.math.roundToLong

actual fun formatCurrency(value: Double): String {
    if (!value.isFinite()) return "Valor indisponível"
    val cents = (abs(value) * 100).roundToLong()
    val integer = (cents / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    return "${if (value < 0) "-" else ""}R$ $integer,${(cents % 100).toString().padStart(2, '0')}"
}
actual fun formatReportPercent(value: Double): String {
    if (!value.isFinite()) return "0,0"
    val tenths = (abs(value) * 10).roundToLong()
    return "${tenths / 10},${tenths % 10}"
}