package com.example.appfinanceiro.feature.relatorios.utils

import java.text.NumberFormat
import java.util.Locale

actual fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
actual fun formatReportPercent(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value)