package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

internal class MemoryPersistence(initial: SessionData = SessionData()) : SessionPersistence {
    val content = MutableStateFlow(initial)
    var fail = false
    override val data: Flow<SessionData> get() = content
    override suspend fun update(transform: (SessionData) -> SessionData) {
        if (fail) error("storage unavailable")
        content.value = transform(content.value)
    }
}

private class FakeBiometrics(var available: Boolean = false, var accepted: Boolean = false,
    val sensor: BiometricKind = BiometricKind.Generic) : BiometricAuthenticator {
    override fun isAvailable() = available
    override fun kind() = sensor
    override suspend fun authenticate() = accepted
}

private fun response() = LoginResponse("ok", "synthetic-token", UserResponse("Pessoa Teste", "pessoa@example.invalid", "user"))

@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {
    @Test fun offerAndProtectedSessionKeepTheDetectedBiometricSensor() = runTest {
        for (sensor in listOf(BiometricKind.FaceId, BiometricKind.TouchId)) {
            val bio = FakeBiometrics(true, true, sensor)
            val memory = MemoryPersistence()
            val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), bio, backgroundScope)
            runCurrent(); controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
            assertEquals(AppRoute.BiometricOffer, controller.state.value.route)
            assertEquals(sensor, controller.biometricKind)
            assertEquals(if (sensor == BiometricKind.FaceId) "Face ID" else "Touch ID", controller.biometricKind.label)
            controller.finishLogin(true); runCurrent(); controller.lock()
            assertEquals(AppRoute.Locked, controller.state.value.route)
            assertEquals(sensor, controller.biometricKind)
            controller.enterForeground(); controller.unlock(); runCurrent()
            assertEquals(AppRoute.Home, controller.state.value.route)
            controller.logout(); runCurrent()
        }
    }

    @Test fun missingEnrollmentDoesNotOfferOrEnableBiometrics() = runTest {
        val memory = MemoryPersistence()
        val bio = FakeBiometrics(false, true, BiometricKind.FaceId)
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), bio, backgroundScope)
        runCurrent(); controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
        assertEquals(AppRoute.Home, controller.state.value.route)
        assertTrue(memory.content.value.biometricEnabledEmails.isEmpty())
        assertFailsWith<IllegalStateException> { controller.setBiometricEnabled(true) }
        assertTrue(memory.content.value.biometricEnabledEmails.isEmpty())
    }
    @Test fun lateProfileBiometricAuthenticationCannotChangeLoggedOutAccount() = runTest {
        val pending = CompletableDeferred<Boolean>()
        val bio = object : BiometricAuthenticator {
            override fun isAvailable() = true
            override suspend fun authenticate() = withContext(NonCancellable) { pending.await() }
        }
        val memory = MemoryPersistence(SessionData(token = "synthetic", email = "pessoa@example.invalid"))
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), bio, backgroundScope)
        runCurrent(); val activation = backgroundScope.async { runCatching { controller.setBiometricEnabled(true) } }; runCurrent()
        controller.logout(); runCurrent(); pending.complete(true); runCurrent()
        assertTrue(activation.await().isFailure); assertTrue(memory.content.value.biometricEnabledEmails.isEmpty()); assertNull(memory.content.value.token)
    }
    @Test fun profileBiometricToggleRequiresAuthenticationAndAppliesOnBackground() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic", email = "pessoa@example.invalid"))
        val bio = FakeBiometrics(true, false)
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), bio, backgroundScope)
        runCurrent()
        assertFailsWith<IllegalStateException> { controller.setBiometricEnabled(true) }
        assertTrue(memory.content.value.biometricEnabledEmails.isEmpty())
        bio.accepted = true; controller.setBiometricEnabled(true); controller.lock()
        assertEquals(AppRoute.Locked, controller.state.value.route)
        controller.enterForeground(); controller.unlock(); runCurrent(); controller.setBiometricEnabled(false); controller.lock()
        assertEquals(AppRoute.Home, controller.state.value.route)
        assertTrue(memory.content.value.biometricEnabledEmails.isEmpty())
    }
    @Test fun backgroundDuringProfileActivationCannotLeaveNewlyProtectedSessionOpen() = runTest {
        val accepted = CompletableDeferred<Boolean>()
        val bio = object : BiometricAuthenticator {
            override fun isAvailable() = true
            override suspend fun authenticate() = accepted.await()
        }
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(MemoryPersistence(SessionData(token = "synthetic", email = "pessoa@example.invalid"))), bio, backgroundScope)
        runCurrent(); backgroundScope.launch { controller.setBiometricEnabled(true) }; runCurrent()
        controller.lock(); accepted.complete(true); runCurrent()
        assertEquals(AppRoute.Locked, controller.state.value.route)
    }
    @Test fun accountRoutesAreRestrictedToUnauthenticatedScreens() = runTest {
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(MemoryPersistence()), FakeBiometrics(), backgroundScope)
        runCurrent(); controller.accountRoute(AppRoute.Register); assertEquals(AppRoute.Register, controller.state.value.route)
        controller.accountRoute(AppRoute.Recovery); assertEquals(AppRoute.Recovery, controller.state.value.route)
        controller.accountRoute(AppRoute.Login); controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
        controller.accountRoute(AppRoute.Register); assertEquals(AppRoute.Home, controller.state.value.route)
    }
    @Test fun restoresWithoutEmittingLoginBeforeSecureRead() = runTest {
        val loaded = CompletableDeferred<SessionData>()
        val persistence = object : SessionPersistence {
            override val data = flow { emit(loaded.await()) }
            override suspend fun update(transform: (SessionData) -> SessionData) = Unit
        }
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(persistence), FakeBiometrics(), backgroundScope)
        runCurrent()
        assertEquals(AppRoute.Loading, controller.state.value.route)
        loaded.complete(SessionData(token = "synthetic-token"))
        runCurrent()
        assertEquals(AppRoute.Home, controller.state.value.route)
    }

    @Test fun loginPersistsIdentityAndLogoutPreservesBiometricChoices() = runTest {
        val memory = MemoryPersistence(SessionData(biometricEnabledEmails = setOf("other@example.invalid")))
        val controller = AppController({ email, password ->
            assertEquals("pessoa@example.invalid", email); assertEquals(" password ", password); response()
        }, PersistentSessionStore(memory), FakeBiometrics(), backgroundScope)
        runCurrent()
        controller.login(" pessoa@example.invalid ", " password ")
        runCurrent()
        assertEquals(AppRoute.Home, controller.state.value.route)
        assertEquals("synthetic-token", memory.content.value.token)
        controller.logout()
        runCurrent()
        assertEquals(AppRoute.Login, controller.state.value.route)
        assertNull(memory.content.value.token)
        assertEquals(setOf("other@example.invalid"), memory.content.value.biometricEnabledEmails)
    }

    @Test fun biometricFailureNeverOpensSavedSessionAndPasswordRemovesOldToken() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic-token", email = "pessoa@example.invalid",
            biometricEnabledEmails = setOf("pessoa@example.invalid")))
        val bio = FakeBiometrics(true, false)
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), bio, backgroundScope)
        runCurrent()
        assertEquals(AppRoute.Locked, controller.state.value.route)
        assertNull(controller.attachHome { error("must not expose token") })
        controller.unlock(); runCurrent()
        assertEquals(AppRoute.Locked, controller.state.value.route)
        bio.accepted = true
        controller.unlock(); runCurrent()
        assertEquals(AppRoute.Home, controller.state.value.route)
        controller.lock()
        assertEquals(AppRoute.Locked, controller.state.value.route)
        controller.logout(); runCurrent()
        assertNull(memory.content.value.token)
    }

    @Test fun enablingBiometricsRequiresSuccessfulSystemAuthentication() = runTest {
        val memory = MemoryPersistence()
        val bio = FakeBiometrics(true, false)
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), bio, backgroundScope)
        runCurrent(); controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
        assertEquals(AppRoute.BiometricOffer, controller.state.value.route)
        controller.finishLogin(true); runCurrent()
        assertEquals(AppRoute.BiometricOffer, controller.state.value.route)
        assertNull(memory.content.value.token)
        bio.accepted = true
        controller.lock() // background during login offer
        controller.finishLogin(true); runCurrent()
        assertEquals(AppRoute.Locked, controller.state.value.route)
        assertTrue("pessoa@example.invalid" in memory.content.value.biometricEnabledEmails)
        controller.enterForeground(); controller.unlock(); runCurrent()
        assertEquals(AppRoute.Home, controller.state.value.route)
    }

    @Test fun rateLimitIsPerEmailAndExpiresAfterFiveMinutes() = runTest {
        var time = 1000L
        var calls = 0
        val controller = AppController({ _, _ -> calls++; throw ApiResponseException(429, "Aguarde.") },
            PersistentSessionStore(MemoryPersistence()), FakeBiometrics(), backgroundScope, { time })
        runCurrent(); controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
        controller.login(" PESSOA@example.invalid ", "synthetic"); runCurrent()
        assertEquals(1, calls)
        controller.login("outra@example.invalid", "synthetic"); runCurrent()
        assertEquals(2, calls)
        time += 300_001
        controller.login("outra@example.invalid", "synthetic"); runCurrent()
        assertEquals(3, calls)
    }

    @Test fun canceledLoginCannotRestoreLoggedOutAccount() = runTest {
        val pending = CompletableDeferred<LoginResponse>()
        val memory = MemoryPersistence()
        val controller = AppController({ _, _ -> withContext(NonCancellable) { pending.await() } },
            PersistentSessionStore(memory), FakeBiometrics(), backgroundScope)
        runCurrent(); controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
        controller.logout(); runCurrent()
        pending.complete(response()); runCurrent()
        assertEquals(AppRoute.Login, controller.state.value.route)
        assertNull(memory.content.value.token)
    }

    @Test fun failedLogoutRetryCannotRestoreTheOldSession() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic-token"))
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), FakeBiometrics(), backgroundScope)
        runCurrent(); memory.fail = true; controller.logout(); runCurrent()
        assertEquals(AppRoute.StorageError, controller.state.value.route)
        memory.fail = false; controller.restore(); runCurrent()
        assertEquals(AppRoute.Login, controller.state.value.route)
        assertNull(memory.content.value.token)
    }

    @Test fun emptyLoginDoesNotCallNetwork() = runTest {
        val controller = AppController({ _, _ -> error("must not call") }, PersistentSessionStore(MemoryPersistence()), FakeBiometrics(), backgroundScope)
        runCurrent(); controller.login("", ""); runCurrent()
        assertEquals("Preencha e-mail e senha.", controller.state.value.message)
        assertFalse(controller.state.value.busy)
    }

    @Test fun deniedLoginWithStorageFailureCanRetryLogoutSafely() = runTest {
        val memory = MemoryPersistence()
        val controller = AppController({ _, _ -> throw ApiResponseException(403, "Acesso negado.") },
            PersistentSessionStore(memory), FakeBiometrics(), backgroundScope)
        runCurrent(); memory.fail = true
        controller.login("pessoa@example.invalid", "synthetic"); runCurrent()
        assertEquals(AppRoute.StorageError, controller.state.value.route)
        memory.fail = false; controller.restore(); runCurrent()
        assertEquals(AppRoute.Login, controller.state.value.route)
    }

    @Test fun revokedAccessEndsTheSessionAndKeepsTheServerMessage() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic-token"))
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), FakeBiometrics(), backgroundScope)
        runCurrent()
        com.example.appfinanceiro.core.network.SessionAccessEvents.notifyAccessRevoked("Acesso revogado.")
        runCurrent()
        assertEquals(AppRoute.Login, controller.state.value.route)
        assertEquals("Acesso revogado.", controller.state.value.message)
        assertNull(memory.content.value.token)
    }

    @Test fun switchingAppsPreservesFinancialWorkspaceAndDrafts() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic-token", email = "pessoa@example.invalid",
            biometricEnabledEmails = setOf("pessoa@example.invalid")))
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), FakeBiometrics(true, true), backgroundScope)
        val fake = EditingFake()
        runCurrent(); controller.unlock(); runCurrent()
        val home = controller.attachHome { HomePresenter(fake, it, backgroundScope, Period(10, 2026)) {} }
        val workspace = controller.attachWorkspace { token, presenter -> FinanceWorkspace(presenter, fake, fake, token, backgroundScope) {} }!!
        workspace.editExpense(1); runCurrent()
        val editor = workspace.expenseEditor
        assertNotNull(editor)
        controller.enterBackground(); runCurrent()
        controller.enterForeground(); runCurrent()
        assertEquals(AppRoute.Home, controller.state.value.route)
        assertSame(home, controller.home)
        assertSame(workspace, controller.workspace)
        assertSame(editor, workspace.expenseEditor)
    }

    @Test fun biometricLockDisposesTheFinancialWorkspaceAndDrafts() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic-token", email = "pessoa@example.invalid",
            biometricEnabledEmails = setOf("pessoa@example.invalid")))
        val controller = AppController({ _, _ -> response() }, PersistentSessionStore(memory), FakeBiometrics(true, true), backgroundScope)
        val fake = EditingFake()
        runCurrent(); controller.unlock(); runCurrent()
        controller.attachHome { HomePresenter(fake, it, backgroundScope, Period(10, 2026)) {} }
        val workspace = controller.attachWorkspace { token, home -> FinanceWorkspace(home, fake, fake, token, backgroundScope) {} }!!
        workspace.editExpense(1); runCurrent()
        assertNotNull(workspace.expenseEditor)
        controller.lock(); runCurrent()
        assertEquals(AppRoute.Locked, controller.state.value.route)
        assertNull(controller.workspace)
        assertNull(workspace.expenseEditor)
        assertNull(controller.attachWorkspace { _, _ -> error("Locked session must not expose its token") })
    }
}
