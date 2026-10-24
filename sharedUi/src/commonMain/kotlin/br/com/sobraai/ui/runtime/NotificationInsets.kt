package br.com.sobraai.ui.runtime

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union

/** One bottom obstruction: the larger of the safe area (including IME) and the bar. */
fun notificationInsets(safeArea: WindowInsets, bottomBarHeightPx: Int): WindowInsets =
    safeArea.union(WindowInsets(bottom = bottomBarHeightPx.coerceAtLeast(0)))
