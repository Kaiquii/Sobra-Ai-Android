package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

fun Modifier.swipeNavigation(
    currentIndex: Int,
    onNavigate: (Int) -> Unit
): Modifier = composed {
    val currentOnNavigate = rememberUpdatedState(onNavigate)
    val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }

    pointerInput(currentIndex, swipeThreshold) {
        var totalDragX = 0f
        var totalDragY = 0f

        detectDragGestures(
            onDragStart = {
                totalDragX = 0f
                totalDragY = 0f
            },
            onDragEnd = {
                val isHorizontalSwipe =
                    abs(totalDragX) >= swipeThreshold &&
                        abs(totalDragX) > abs(totalDragY) * 1.5f

                if (isHorizontalSwipe) {
                    val targetIndex = if (totalDragX < 0f) {
                        currentIndex + 1
                    } else {
                        currentIndex - 1
                    }

                    if (targetIndex in 0..3) {
                        currentOnNavigate.value(targetIndex)
                    }
                }
            },
            onDragCancel = {
                totalDragX = 0f
                totalDragY = 0f
            },
            onDrag = { _, dragAmount ->
                totalDragX += dragAmount.x
                totalDragY += dragAmount.y
            }
        )
    }
}
