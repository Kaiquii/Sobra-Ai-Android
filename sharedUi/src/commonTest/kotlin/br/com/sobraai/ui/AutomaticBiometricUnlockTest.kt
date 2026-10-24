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

    @Test fun switchingAppsPreservesUnlockedSessionWithoutAnotherBiometricAttempt() = runTest {
        val bio = Biometrics()
        val app = controller(backgroundScope, bio)
        app.enterForeground(); runCurrent()
        repeat(3) {
            app.enterBackground(); runCurrent()
            assertEquals(AppRoute.Home, app.state.value.route)
            app.enterForeground(); runCurrent()
            assertEquals(AppRoute.Home, app.state.value.route)
            assertEquals(1, bio.calls)
        }
    }

    @Test fun newControllerStillRequiresBiometricsAfterPreviousSessionWasUnlocked() = runTest {
        val bio = Biometrics()
        val memory = MemoryPersistence(savedSession())
        val previous = controller(backgroundScope, bio, memory)
        previous.enterForeground(); runCurrent()
        previous.enterBackground(); runCurrent()
        val reopened = controller(backgroundScope, bio, memory)
        runCurrent()
        assertEquals(AppRoute.Locked, reopened.state.value.route)
        reopened.enterForeground(); runCurrent()
        assertEquals(2, bio.calls)
        assertEquals(AppRoute.Home, reopened.state.value.route)
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
        app.enterBackground(); runCurrent()
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

    @Test fun canceledBiometricsAllowPasswordLoginWithoutAnIntermediateUnlockAction() = runTest {
        val bio = Biometrics().apply { result = { false } }
        val memory = MemoryPersistence(savedSession())
        var requests = 0
        val app = AppController({ email, password ->
            requests++
            assertNull(memory.content.value.token)
            assertEquals("outra@example.invalid", email)
            assertEquals("synthetic-password", password)
            com.example.appfinanceiro.core.network.auth.LoginResponse("ok", "new-synthetic-token",
                com.example.appfinanceiro.core.network.auth.UserResponse("Pessoa", email, "user"))
        }, PersistentSessionStore(memory), bio, backgroundScope)
        app.enterForeground(); runCurrent()
        assertEquals(AppRoute.Locked, app.state.value.route)
        app.login("outra@example.invalid", "synthetic-password"); runCurrent()
        assertEquals(1, requests)
        assertEquals(1, bio.calls)
        assertEquals(AppRoute.BiometricOffer, app.state.value.route)
        assertNull(memory.content.value.token)
        app.finishLogin(false); runCurrent()
        assertEquals(AppRoute.Home, app.state.value.route)
        assertEquals("new-synthetic-token", memory.content.value.token)
    }

    @Test fun rejectedPasswordCannotRestoreTheOldProtectedSession() = runTest {
        val bio = Biometrics().apply { result = { false } }
        val memory = MemoryPersistence(savedSession())
        val app = AppController({ _, _ -> throw ApiResponseException(401, "Credenciais inválidas.") },
            PersistentSessionStore(memory), bio, backgroundScope)
        app.enterForeground(); runCurrent()
        app.login("pessoa@example.invalid", "synthetic-invalid"); runCurrent()
        assertEquals(AppRoute.Login, app.state.value.route)
        assertFalse(app.state.value.busy)
        assertNull(memory.content.value.token)
        app.enterForeground(); runCurrent()
        assertEquals(1, bio.calls)
        assertEquals(AppRoute.Login, app.state.value.route)
    }

    @Test fun passwordFallbackDoesNotSubmitCredentialsIfSecureSessionCannotBeCleared() = runTest {
        val bio = Biometrics().apply { result = { false } }
        val memory = MemoryPersistence(savedSession())
        var requests = 0
        val app = AppController({ _, _ -> requests++; error("No request expected") },
            PersistentSessionStore(memory), bio, backgroundScope)
        app.enterForeground(); runCurrent()
        memory.fail = true
        app.login("pessoa@example.invalid", "synthetic-password"); runCurrent()
        assertEquals(0, requests)
        assertEquals(AppRoute.StorageError, app.state.value.route)
        assertNull(app.attachHome { error("Protected data must stay inaccessible") })
    }

    @Test fun recoveryAndRegistrationFromProtectedLoginClearSavedSessionFirst() = runTest {
        for (route in listOf(AppRoute.Recovery, AppRoute.Register)) {
            val bio = Biometrics().apply { result = { false } }
            val memory = MemoryPersistence(savedSession())
            val app = controller(backgroundScope, bio, memory)
            app.enterForeground(); runCurrent()
            app.accountRoute(route); runCurrent()
            assertEquals(route, app.state.value.route)
            assertNull(memory.content.value.token)
            app.enterForeground(); runCurrent()
            assertEquals(1, bio.calls)
        }
    }
}
