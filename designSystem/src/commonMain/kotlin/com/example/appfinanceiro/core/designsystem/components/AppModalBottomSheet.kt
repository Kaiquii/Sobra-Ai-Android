package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

private class BottomSheetKeyboardInsets {
    var current: WindowInsets? = null
}

/** Keeps swipe dismissal available without discarding input while the keyboard is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false,
    dismissEnabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable ColumnScope.() -> Unit
) {
    val keyboardInsets = remember { BottomSheetKeyboardInsets() }
    var keyboardVisible by remember { mutableStateOf(true) }
    val currentDensity by rememberUpdatedState(LocalDensity.current)
    val currentDismissEnabled by rememberUpdatedState(dismissEnabled)
    // Read current insets in callbacks too: the remembered SheetState must not capture
    // the keyboard visibility from the first composition or lag behind its animation.
    val canDismiss = {
        currentDismissEnabled && keyboardInsets.current?.getBottom(currentDensity) == 0
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = { it != SheetValue.Hidden || canDismiss() }
    )

    ModalBottomSheet(
        onDismissRequest = { if (canDismiss()) onDismissRequest() },
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = dismissEnabled && !keyboardVisible,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        // Android hosts the sheet in its own dialog window. Observe that window's
        // IME rather than the underlying screen's insets; this is common on iOS too.
        val dialogKeyboardInsets = WindowInsets.ime
        val dialogKeyboardVisible = dialogKeyboardInsets.getBottom(LocalDensity.current) > 0
        SideEffect {
            // iOS may return a fresh WindowInsets wrapper on each composition. Keep
            // its reference outside snapshot state to avoid a recomposition loop.
            keyboardInsets.current = dialogKeyboardInsets
            keyboardVisible = dialogKeyboardVisible
        }
        content()
    }
}
