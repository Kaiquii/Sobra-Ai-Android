package br.com.sobraai.ui

import com.example.appfinanceiro.core.update.*
import kotlin.test.*

class AppUpdatePresentationTest {
    private fun release(min: Int = 100, force: Boolean = false) = AppRelease(141, "1.4.1", min, force, "Melhorias", "2026-10-22", "https://example.invalid/store")
    @Test fun currentVersionDoesNotShowAnOptionalUpdate() { assertNull(appUpdateState("1.4.1", 141, release())) }
    @Test fun newerReleaseUsesTheSameOptionalPresentation() {
        val state = assertNotNull(appUpdateState("1.4.0", 140, release()))
        assertFalse(state.mustUpdate)
        assertEquals(140, state.installedVersionCode)
        assertEquals(141, state.version.latestVersionCode)
    }
    @Test fun minimumVersionRequiresUpdate() { assertTrue(assertNotNull(appUpdateState("1.0", 99, release())).mustUpdate) }
    @Test fun serverForceFlagRemainsMandatory() { assertTrue(assertNotNull(appUpdateState("1.4.1", 141, release(force = true))).mustUpdate) }
}
