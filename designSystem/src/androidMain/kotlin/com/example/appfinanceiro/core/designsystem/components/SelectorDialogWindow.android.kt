package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.ui.window.DialogProperties

actual fun selectorDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    // Compose owns the animated IME insets; the floating window must not also pan/resize.
    decorFitsSystemWindows = false
)
