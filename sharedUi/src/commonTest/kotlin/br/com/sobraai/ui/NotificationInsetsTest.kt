package br.com.sobraai.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import br.com.sobraai.ui.runtime.notificationInsets
import kotlin.test.*

class NotificationInsetsTest {
    private val density = Density(1f)

    @Test fun keyboardClosingStopsAboveTheBarInsteadOfDroppingBelowAndJumpingBack() {
        val navigation = WindowInsets(bottom = 24)
        val positions = listOf(320, 250, 180, 130, 100, 50, 1, 0).map { ime ->
            notificationInsets(navigation.union(WindowInsets(bottom = ime)), 126).getBottom(density)
        }
        assertEquals(listOf(320, 250, 180, 130, 126, 126, 126, 126), positions)
        assertTrue(positions.zipWithNext().all { (previous, next) -> next <= previous })
    }

    @Test fun appearingDestinationBarDoesNotAddItsHeightOnTopOfTheKeyboard() {
        val positions = listOf(300 to 0, 260 to 126, 170 to 126, 60 to 126, 0 to 126).map { (ime, bar) ->
            notificationInsets(WindowInsets(bottom = ime), bar).getBottom(density)
        }
        assertEquals(listOf(300, 260, 170, 126, 126), positions)
    }

    @Test fun formsWithoutBottomBarRespectImeNavigationAndHorizontalSafeAreas() {
        val safe = WindowInsets(left = 10, top = 30, right = 12, bottom = 240)
        val insets = notificationInsets(safe, 0)
        assertEquals(240, insets.getBottom(density))
        assertEquals(10, insets.getLeft(density, LayoutDirection.Ltr))
        assertEquals(12, insets.getRight(density, LayoutDirection.Ltr))
        assertEquals(30, insets.getTop(density))
        assertEquals(24, notificationInsets(WindowInsets(bottom = 24), 0).getBottom(density))
    }

    @Test fun reopeningKeyboardUsesSameAnchorWithoutAnOffsetDiscontinuity() {
        val positions = listOf(0, 1, 40, 125, 126, 160, 300).map {
            notificationInsets(WindowInsets(bottom = it), 126).getBottom(density)
        }
        assertEquals(listOf(126, 126, 126, 126, 126, 160, 300), positions)
    }
}
