package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = PrimaryBlue
) {
    CircularProgressIndicator(
        modifier = modifier.then(Modifier.size(size)),
        color = color,
        strokeWidth = strokeWidth
    )
}
