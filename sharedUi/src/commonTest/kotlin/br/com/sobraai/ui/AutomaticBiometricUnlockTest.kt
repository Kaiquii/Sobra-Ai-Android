package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AutomaticBiometricUnlockTest {
    private class Biometrics(val sensor: BiometricKind = BiometricKind.FaceId) : BiometricAuthenticator {
        var calls = 0
        var result: suspend () -> Boolean = { true }
        override fun isAvailable() = true
        override fun kind() = sensor
        override suspend fun authenticate(): Boolean { calls++; return result() }
    }

    private fun savedSession() = SessionData(
        token = "synthetic-token", email = "pessoa@example.invalid",
        biometricEnabledEmails = setOf("pessoa@example.invalid")
    )

    private fun controller(scope: CoroutineScope, biometrics: Biometrics,
        persistence: SessionPersistence = MemoryPersistence(savedSession())) =
        AppController({ _, _ -> error("Saved sessions must not submit credentials") },
            PersistentSessionStore(persistence), biometrics, scope)

    @Test fun savedSessionAuthenticatesAutomaticallyOnlyOnceAfterForegroundForEverySensor() = runTest {
        for (sensor in BiometricKind.entries) {
            val bio = Biometrics(sensor)
            val app = controller(backgroundScope, bio)
            runCurrent()
            assertEquals(AppRoute.Locked, app.state.value.route)
            assertEquals(0, bio.calls)
            app.enterForeground(); runCurrent()
            assertEquals(AppRoute.Home, app.state.value.route)
            assertEquals(1, bio.calls)
            app.enterForeground(); runCurrent()
            assertEquals(1, bio.calls)
        }
    }

    @Test fun foregroundBeforeSecureStorageReadStillAuthenticatesOnceWhenSessionArrives() = runTest {
        val loaded = CompletableDeferred<SessionData>()
        val persistence = object : SessionPersistence {
            override val data = flow { emit(loaded.await()) }
            override suspend fun update(transform: (SessionData) -> SessionData) = Unit
        }
        val bio = Biometrics()
        val app = controller(backgroundScope, bio, persistence)
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Loading, app.state.value.route)
        assertEquals(0, bio.calls)
        loaded.complete(savedSession()); runCurrent()
        assertEquals(AppRoute.Home, app.state.value.route)
        assertEquals(1, bio.calls)
    }

    @Test fun canceledPromptDoesNotLoopOnActiveCallbacksAndManualRetryStillWorks() = runTest {
        val bio = Biometrics().apply { result = { false } }
        val app = controller(backgroundScope, bio)
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Locked, app.state.value.route)
        assertNotNull(app.state.value.message)
        repeat(3) { app.enterForeground(); runCurrent() }
        assertEquals(1, bio.calls)
        bio.result = { true }
        app.unlock(); runCurrent()
        assertEquals(AppRoute.Home, app.state.value.route)
        assertEquals(2, bio.calls)
    }

    @Test fun authenticationErrorKeepsSessionLockedAndAllowsPasswordFallback() = runTest {
        val bio = Biometrics().apply { result = { error("sensor unavailable") } }
        val memory = MemoryPersistence(savedSession())
        val app = controller(backgroundScope, bio, memory)
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Locked, app.state.value.route)
        assertFalse(app.state.value.busy)
        app.enterForeground(); runCurrent()
        assertEquals(1, bio.calls)
        app.logout(); runCurrent()
        assertEquals(AppRoute.Login, app.state.value.route)
        assertNull(memory.content.value.token)
    }

    @Test fun backgroundLocksContentAndReturningStartsAnotherAutomaticAttempt() = runTest {
        val bio = Biometrics()
        val app = controller(backgroundScope, bio)
        app.enterForeground(); runCurrent()
        app.lock(); runCurrent()
        assertEquals(AppRoute.Locked, app.state.value.route)
        assertEquals(1, bio.calls)
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Home, app.state.value.route)
        assertEquals(2, bio.calls)
    }

    @Test fun inactiveCallbacksDuringPromptDoNotStartConcurrentAuthentication() = runTest {
        val accepted = CompletableDeferred<Boolean>()
        val bio = Biometrics().apply { result = { accepted.await() } }
        val app = controller(backgroundScope, bio)
        app.enterForeground(); runCurrent()
        repeat(3) { app.enterForeground(); app.unlock(); runCurrent() }
        assertEquals(1, bio.calls)
        assertTrue(app.state.value.busy)
        accepted.complete(true); runCurrent()
        assertEquals(AppRoute.Home, app.state.value.route)
    }

    @Test fun backgroundDuringPromptRejectsLateSuccessAndAuthenticatesAgainOnReturn() = runTest {
        val accepted = CompletableDeferred<Boolean>()
        val bio = Biometrics().apply { result = { withContext(NonCancellable) { accepted.await() } } }
        val app = controller(backgroundScope, bio)
        app.enterForeground(); runCurrent()
        app.lock(); runCurrent()
        accepted.complete(true); runCurrent()
        assertEquals(AppRoute.Locked, app.state.value.route)
        assertFalse(app.state.value.busy)
        bio.result = { true }
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Home, app.state.value.route)
        assertEquals(2, bio.calls)
    }

    @Test fun logoutDuringAutomaticPromptCannotRestoreTheOldSession() = runTest {
        val accepted = CompletableDeferred<Boolean>()
        val bio = Biometrics().apply { result = { withContext(NonCancellable) { accepted.await() } } }
        val memory = MemoryPersistence(savedSession())
        val app = controller(backgroundScope, bio, memory)
        app.enterForeground(); runCurrent()
        app.logout(); runCurrent()
        accepted.complete(true); runCurrent()
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Login, app.state.value.route)
        assertNull(memory.content.value.token)
        assertEquals(1, bio.calls)
    }

    @Test fun noAutomaticPromptWithoutSavedSessionOrBiometricOptIn() = runTest {
        for (session in listOf(savedSession().copy(token = null), savedSession().copy(biometricEnabledEmails = emptySet()))) {
            val bio = Biometrics()
            val app = controller(backgroundScope, bio, MemoryPersistence(session))
            app.enterForeground(); runCurrent()
            assertEquals(if (session.token == null) AppRoute.Login else AppRoute.Home, app.state.value.route)
            assertEquals(0, bio.calls)
        }
    }
}
