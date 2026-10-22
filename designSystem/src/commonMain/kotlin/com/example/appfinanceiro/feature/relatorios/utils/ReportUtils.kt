package com.example.appfinanceiro.feature.relatorios.utils

import androidx.compose.ui.graphics.Color
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue



expect fun formatCurrency(value: Double): String
expect fun formatReportPercent(value: Double): String

fun monthShortName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Fev"
        3 -> "Mar"
        4 -> "Abr"
        5 -> "Mai"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Ago"
        9 -> "Set"
        10 -> "Out"
        11 -> "Nov"
        12 -> "Dez"
        else -> "-"
    }
}

fun categoryColor(index: Int): Color {
    val colors = listOf(
        PrimaryBlue,
        Color(0xFF9C27B0),
        Color(0xFFFF9800),
        Color(0xFF00BCD4),
        Color(0xFF4CAF50),
        Color(0xFFE91E63),
        Color(0xFFFFC107)
    )
    return colors[index % colors.size]
}
