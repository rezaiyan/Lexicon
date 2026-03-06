package data.profile.repository

import core.common.Try
import core.common.getOrThrow
import data.auth.remote.model.UserDto
import data.profile.remote.IProfileRemoteDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileRepositoryImplTest {

    private val remoteDataSource = FakeProfileRemoteDataSource()

    private fun createRepo() = ProfileRepositoryImpl(remoteDataSource)

    private val testUserDto = UserDto(
        id = 1L, email = "test@test.com", name = "Updated Name",
        subscriptionStatus = "FREE", subscriptionExpiresAt = null
    )

    @Test
    fun `updateProfile returns mapped AuthUser on success`() = runTest {
        remoteDataSource.updateResult = Try.success(testUserDto)
        val repo = createRepo()

        val result = repo.updateProfile("Updated Name", "alias")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("Updated Name", user.name)
        assertEquals(1L, user.id)
    }

    @Test
    fun `updateProfile returns failure on error`() = runTest {
        remoteDataSource.updateResult = Try.failure(RuntimeException("Network error"))
        val repo = createRepo()

        val result = repo.updateProfile("Name", null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `uploadAvatar delegates to remote and returns URL`() = runTest {
        remoteDataSource.avatarResult = Try.success("https://cdn.example.com/avatar.jpg")
        val repo = createRepo()

        val result = repo.uploadAvatar(byteArrayOf(1, 2, 3), "image/jpeg")

        assertTrue(result.isSuccess)
        assertEquals("https://cdn.example.com/avatar.jpg", result.getOrThrow())
    }

    @Test
    fun `uploadAvatar returns failure on error`() = runTest {
        remoteDataSource.avatarResult = Try.failure(RuntimeException("Upload failed"))
        val repo = createRepo()

        val result = repo.uploadAvatar(byteArrayOf(1), "image/png")

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteAvatar delegates to remote`() = runTest {
        remoteDataSource.deleteAvatarResult = Try.success(Unit)
        val repo = createRepo()

        val result = repo.deleteAvatar()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteAvatar returns failure on error`() = runTest {
        remoteDataSource.deleteAvatarResult = Try.failure(RuntimeException("Delete failed"))
        val repo = createRepo()

        val result = repo.deleteAvatar()

        assertTrue(result.isFailure)
    }

    // --- Fakes ---

    private class FakeProfileRemoteDataSource : IProfileRemoteDataSource {
        var updateResult: Try<UserDto> = Try.failure(RuntimeException("not set"))
        var avatarResult: Try<String> = Try.failure(RuntimeException("not set"))
        var deleteAvatarResult: Try<Unit> = Try.success(Unit)

        override suspend fun updateProfile(name: String?, displayAlias: String?): Try<UserDto> = updateResult
        override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String> = avatarResult
        override suspend fun deleteAvatar(): Try<Unit> = deleteAvatarResult
    }
}
