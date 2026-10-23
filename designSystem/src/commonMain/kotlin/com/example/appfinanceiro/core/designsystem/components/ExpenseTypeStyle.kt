package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Presentation only: the API type, never the payment status or category, selects the accent. */
enum class ExpenseTypeStyle(private val light: Color, private val dark: Color) {
    Unique(Color(0xFF1565C0), Color(0xFF60A5FA)),
    Installment(Color(0xFF6D28D9), Color(0xFFB49AFF)),
    Fixed(Color(0xFF00695C), Color(0xFF5EDAC8)),
    Other(Color(0xFF475569), Color(0xFFA0AEC0));

    @Composable
    fun accent(): Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) dark else light
}

fun expenseTypeStyle(apiType: String): ExpenseTypeStyle = when (apiType.trim().lowercase()) {
    "única", "unica" -> ExpenseTypeStyle.Unique
    "parcelada" -> ExpenseTypeStyle.Installment
    "fixa" -> ExpenseTypeStyle.Fixed
    else -> ExpenseTypeStyle.Other
}
