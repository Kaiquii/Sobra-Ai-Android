package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileBiometricPreferenceTest {
    private class Sensor : BiometricAuthenticator {
        var available = true
        var accepted = true
        var calls = 0
        override fun isAvailable() = available
        override suspend fun authenticate(): Boolean { calls++; return accepted }
    }

    private fun app(scope: CoroutineScope, memory: MemoryPersistence, sensor: Sensor) =
        AppController({ _, _ -> error("Changing local preference must not log in") },
            PersistentSessionStore(memory), sensor, scope)

    private val saved = SessionData(token = "synthetic-token", name = "Pessoa",
        email = "pessoa@example.invalid", role = "user", avatarUrl = "synthetic-avatar",
        biometricEnabledEmails = setOf("outra@example.invalid"))

    @Test fun enableAndDisablePreserveTokenIdentityAndOtherAccountPreferences() = runTest {
        val memory = MemoryPersistence(saved)
        val sensor = Sensor()
        val controller = app(backgroundScope, memory, sensor)
        runCurrent()
        controller.setBiometricEnabled(true)
        assertEquals(1, sensor.calls)
        assertEquals(saved.copy(biometricEnabledEmails = saved.biometricEnabledEmails + saved.email), memory.content.value)
        // Disabling remains possible even if the sensor is no longer available.
        sensor.available = false
        controller.setBiometricEnabled(false)
        assertEquals(1, sensor.calls)
        assertEquals(saved, memory.content.value)
        assertEquals(AppRoute.Home, controller.state.value.route)
        val reopened = app(backgroundScope, memory, sensor)
        reopened.enterForeground(); runCurrent()
        assertEquals(AppRoute.Home, reopened.state.value.route)
        assertEquals(1, sensor.calls)
    }

    @Test fun enabledPreferenceRequiresBiometricsOnNextOpening() = runTest {
        val memory = MemoryPersistence(saved)
        val sensor = Sensor()
        val controller = app(backgroundScope, memory, sensor)
        runCurrent(); controller.setBiometricEnabled(true)
        val reopened = app(backgroundScope, memory, sensor)
        runCurrent()
        assertEquals(AppRoute.Locked, reopened.state.value.route)
        reopened.enterForeground(); runCurrent()
        assertEquals(2, sensor.calls)
        assertEquals(AppRoute.Home, reopened.state.value.route)
    }

    @Test fun cancelledActivationAndWriteFailureKeepPreviousPreferenceAndSession() = runTest {
        val memory = MemoryPersistence(saved)
        val sensor = Sensor().apply { accepted = false }
        val controller = app(backgroundScope, memory, sensor)
        runCurrent()
        assertFailsWith<IllegalStateException> { controller.setBiometricEnabled(true) }
        assertEquals(saved, memory.content.value)
        sensor.accepted = true
        memory.fail = true
        assertFailsWith<IllegalStateException> { controller.setBiometricEnabled(true) }
        assertEquals(saved, memory.content.value)
        assertEquals(AppRoute.Home, controller.state.value.route)
        memory.fail = false
        controller.setBiometricEnabled(true)
        memory.fail = true
        assertFailsWith<IllegalStateException> { controller.setBiometricEnabled(false) }
        assertTrue(saved.email in memory.content.value.biometricEnabledEmails)
        assertEquals(saved.token, memory.content.value.token)
    }
}
