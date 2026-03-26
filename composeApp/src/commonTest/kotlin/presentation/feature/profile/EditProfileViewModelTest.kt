package presentation.feature.profile

import core.common.Try
import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import domain.profile.repository.IProfileRepository
import domain.profile.usecase.DeleteAvatarUseCase
import domain.profile.usecase.UpdateProfileUseCase
import domain.profile.usecase.UploadAvatarUseCase
import domain.profile.usecase.ValidateDisplayAliasUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import feature.profile.EditProfileEffect
import feature.profile.EditProfileViewModel
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EditProfileViewModelTest : ViewModelTestBase() {

    private val testUser = AuthUser(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        displayAlias = "tester",
        profileImageUrl = "https://example.com/avatar.jpg"
    )

    private var updateProfileResult: Try<AuthUser> = Try.success(testUser)
    private var uploadAvatarResult: Try<String> = Try.success("https://example.com/new-avatar.jpg")
    private var deleteAvatarResult: Try<Unit> = Try.success(Unit)

    private val userFlow = MutableStateFlow<AuthUser?>(testUser)
    private var lastSetUser: AuthUser? = null

    private fun fakeUserManager() = object : IUserManager {
        override fun observeUser(): Flow<AuthUser?> = userFlow
        override fun setUser(user: AuthUser?) { lastSetUser = user }
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeProfileRepo() = object : IProfileRepository {
        override suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser> = updateProfileResult
        override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String> = uploadAvatarResult
        override suspend fun deleteAvatar(): Try<Unit> = deleteAvatarResult
    }

    private fun createViewModel(): EditProfileViewModel {
        val repo = fakeProfileRepo()
        return EditProfileViewModel(
            userManager = fakeUserManager(),
            updateProfileUseCase = UpdateProfileUseCase(repo),
            uploadAvatarUseCase = UploadAvatarUseCase(repo),
            deleteAvatarUseCase = DeleteAvatarUseCase(repo),
            validateDisplayAliasUseCase = ValidateDisplayAliasUseCase(),
        )
    }

    @Test
    fun `init loads current user into state`() = runTest {
        val vm = createViewModel()
        assertEquals("tester", vm.currentState.displayAlias)
        assertEquals("test@example.com", vm.currentState.email)
        assertEquals("Test User", vm.currentState.name)
        assertEquals("https://example.com/avatar.jpg", vm.currentState.profileImageUrl)
    }

    @Test
    fun `updateDisplayAlias updates state`() {
        val vm = createViewModel()
        vm.updateDisplayAlias("newAlias")
        assertEquals("newAlias", vm.currentState.displayAlias)
    }

    @Test
    fun `saveProfile success emits ProfileSaved`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.updateDisplayAlias("validname")

        vm.saveProfile()

        val effect = vm.effects.first()
        assertIs<EditProfileEffect.ProfileSaved>(effect)
        assertEquals(false, vm.currentState.isSaving)
    }

    @Test
    fun `saveProfile with short alias sets error`() = runTest {
        val vm = createViewModel()
        vm.updateDisplayAlias("a")

        vm.saveProfile()

        assertEquals("Username must be 2-30 characters", vm.currentState.errorMessage)
    }

    @Test
    fun `saveProfile with invalid chars sets error`() = runTest {
        val vm = createViewModel()
        vm.updateDisplayAlias("bad@name!")

        vm.saveProfile()

        assertEquals("Only letters, numbers, spaces, underscores, and hyphens allowed", vm.currentState.errorMessage)
    }

    @Test
    fun `saveProfile failure sets error`() = runTest {
        updateProfileResult = Try.failure(RuntimeException("Network error"))
        val vm = createViewModel()
        vm.updateDisplayAlias("validname")

        vm.saveProfile()

        assertEquals("Network error", vm.currentState.errorMessage)
        assertEquals(false, vm.currentState.isSaving)
    }

    @Test
    fun `uploadAvatar success updates profileImageUrl`() = runTest {
        val vm = createViewModel()

        vm.uploadAvatar(byteArrayOf(1, 2, 3), "image/jpeg")

        assertEquals("https://example.com/new-avatar.jpg", vm.currentState.profileImageUrl)
        assertEquals(false, vm.currentState.isUploadingAvatar)
    }

    @Test
    fun `uploadAvatar failure sets error`() = runTest {
        uploadAvatarResult = Try.failure(RuntimeException("Upload failed"))
        val vm = createViewModel()

        vm.uploadAvatar(byteArrayOf(1, 2, 3), "image/jpeg")

        assertEquals("Upload failed", vm.currentState.errorMessage)
        assertEquals(false, vm.currentState.isUploadingAvatar)
    }

    @Test
    fun `deleteAvatar success clears profileImageUrl`() = runTest {
        val vm = createViewModel()

        vm.deleteAvatar()

        assertNull(vm.currentState.profileImageUrl)
        assertEquals(false, vm.currentState.isUploadingAvatar)
    }

    @Test
    fun `deleteAvatar failure sets error`() = runTest {
        deleteAvatarResult = Try.failure(RuntimeException("Delete failed"))
        val vm = createViewModel()

        vm.deleteAvatar()

        assertEquals("Delete failed", vm.currentState.errorMessage)
    }

    @Test
    fun `dismissError clears error`() {
        val vm = createViewModel()
        vm.updateDisplayAlias("a")
        vm.saveProfile()
        assertEquals("Username must be 2-30 characters", vm.currentState.errorMessage)

        vm.dismissError()
        assertNull(vm.currentState.errorMessage)
    }
}
