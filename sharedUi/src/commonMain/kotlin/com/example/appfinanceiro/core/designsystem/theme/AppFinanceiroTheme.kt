package com.example.appfinanceiro.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
fun AppFinanceiroTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    PlatformWindowAppearance(darkTheme)
    SharedFinanceTheme(darkTheme, content)
}
@Composable
internal expect fun PlatformWindowAppearance(darkTheme: Boolean)
