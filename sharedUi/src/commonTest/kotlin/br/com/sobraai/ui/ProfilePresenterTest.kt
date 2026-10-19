package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

private open class ProfileFake : ProfileDataSource {
    var user = ProfileUser(1, "Pessoa Teste", "pessoa@example.invalid", "user")
    var expired = false
    var deletedPhotos = 0
    val changes = mutableListOf<String>()
    override suspend fun getProfile(token: String): ProfileResponse { if (expired) throw SessionExpiredException(); return ProfileResponse(user) }
    override suspend fun updateProfile(token: String, request: UpdateProfileRequest): DefaultResponse { user = user.copy(name = request.name, email = request.email); return DefaultResponse("ok") }
    override suspend fun getCategories(token: String) = CategoriesResponse(listOf(Category(1, name = "Casa")), 1)
    override suspend fun createCategory(token: String, request: CategoryRequest): CategoryResponse { changes += "create:${request.name}"; return CategoryResponse(Category(2, name = request.name), "ok") }
    override suspend fun updateCategory(token: String, id: Int, request: CategoryRequest): DefaultResponse { changes += "update:$id:${request.name}"; return DefaultResponse("ok") }
    override suspend fun deleteCategory(token: String, id: Int): DefaultResponse { changes += "delete:$id"; return DefaultResponse("ok") }
    override suspend fun deleteProfilePhoto(token: String): DefaultResponse { deletedPhotos++; return DefaultResponse("ok") }
}
@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePresenterTest {
    @Test fun canceledProfileSaveCannotRestoreLoggedOutTokenAfterLateResponse() = runTest {
        val pending = CompletableDeferred<Unit>()
        val source = object : ProfileFake() { override suspend fun updateProfile(token: String, request: UpdateProfileRequest): DefaultResponse {
            withContext(NonCancellable) { pending.await() }; return DefaultResponse("ok")
        } }
        val memory = MemoryPersistence(SessionData(token = "synthetic", email = "pessoa@example.invalid")); val store = PersistentSessionStore(memory)
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val p = ProfilePresenter(source, FilesFake(), NativeFake(), store, "synthetic", scope, {}, {}, {}, {})
        p.save("Nova Pessoa", "new@example.invalid"); runCurrent(); scope.cancel(); store.clearSession(); pending.complete(Unit); runCurrent()
        assertNull(memory.content.value.token); assertEquals("", memory.content.value.email)
    }
    @Test fun emailEditUpdatesSecureIdentityAndPreservesAccountBiometricPreference() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic", email = "pessoa@example.invalid", biometricEnabledEmails = setOf("pessoa@example.invalid")))
        val source = ProfileFake(); var identityRefresh = 0
        val p = ProfilePresenter(source, FilesFake(), NativeFake(), PersistentSessionStore(memory), "synthetic", backgroundScope, {}, {}, {}, { identityRefresh++ })
        p.save(" Nova Pessoa ", " nova@example.invalid "); p.save("Outra", "other@example.invalid"); runCurrent()
        assertEquals("Nova Pessoa", memory.content.value.name); assertEquals("nova@example.invalid", memory.content.value.email)
        assertTrue("nova@example.invalid" in memory.content.value.biometricEnabledEmails); assertEquals(1, identityRefresh)
        assertEquals("synthetic", memory.content.value.token)
    }
    @Test fun cancelingPhotoSelectionMakesNoUploadOrSessionChange() = runTest {
        var uploads = 0
        val files = object : FilesFake() { override suspend fun uploadPhoto(token: String, photo: ProfilePhoto): ProfilePhotoResponse { uploads++; return super.uploadPhoto(token, photo) } }
        val memory = MemoryPersistence(SessionData(token = "synthetic", avatarUrl = "/original.jpg"))
        val p = ProfilePresenter(ProfileFake(), files, NativeFake(), PersistentSessionStore(memory), "synthetic", backgroundScope, {}, {}, {}, {})
        p.photo(); runCurrent(); assertEquals(0, uploads); assertEquals("/original.jpg", memory.content.value.avatarUrl)
        assertNull(p.action.state.value.error)
    }
    @Test fun successfulPhotoRemovalClearsSecureAvatarAndPreview() = runTest {
        val memory = MemoryPersistence(SessionData(token = "synthetic", avatarUrl = "/original.jpg")); val source = ProfileFake()
        val p = ProfilePresenter(source, FilesFake(), NativeFake(), PersistentSessionStore(memory), "synthetic", backgroundScope, {}, {}, {}, {})
        p.loadPhoto("/original.jpg"); runCurrent(); assertNotNull(p.avatar.value)
        p.removePhoto(); runCurrent(); assertEquals("", memory.content.value.avatarUrl); assertNull(p.avatar.value); assertEquals(1, source.deletedPhotos)
    }
    @Test fun categoryMutationsInvalidateFinancialListsAndRetainApiIdentifiers() = runTest {
        val source = ProfileFake(); var invalidations = 0
        val p = ProfilePresenter(source, FilesFake(), NativeFake(), PersistentSessionStore(MemoryPersistence()), "synthetic", backgroundScope, {}, { invalidations++ }, {}, {})
        p.category(" "); runCurrent(); assertTrue(source.changes.isEmpty())
        p.category(" Nova "); runCurrent(); p.category(" Casa nova ", Category(1, name = "Casa")); runCurrent(); p.deleteCategory(Category(1, name = "Casa nova")); runCurrent()
        assertEquals(listOf("create:Nova", "update:1:Casa nova", "delete:1"), source.changes); assertEquals(3, invalidations)
    }
    @Test fun expiredProfileTriggersLogoutWithoutFakeLoadedData() = runTest {
        var expired = 0; val source = ProfileFake().also { it.expired = true }
        val p = ProfilePresenter(source, FilesFake(), NativeFake(), PersistentSessionStore(MemoryPersistence()), "synthetic", backgroundScope, { expired++ }, {}, {}, {})
        p.open(); runCurrent(); assertEquals(1, expired); assertNull(p.profile.state.value.data)
    }
    @Test fun canceledAvatarRequestCannotRestoreRemovedPreview() = runTest {
        val pending = CompletableDeferred<ByteArray>()
        val files = object : FilesFake() { override suspend fun avatar(token: String, reference: String): ByteArray = withContext(NonCancellable) { pending.await() } }
        val p = ProfilePresenter(ProfileFake(), files, NativeFake(), PersistentSessionStore(MemoryPersistence()), "synthetic", backgroundScope, {}, {}, {}, {})
        p.loadPhoto("/old.jpg"); runCurrent(); p.removePhoto(); runCurrent(); pending.complete(byteArrayOf(1)); runCurrent()
        assertNull(p.avatar.value)
    }
}
