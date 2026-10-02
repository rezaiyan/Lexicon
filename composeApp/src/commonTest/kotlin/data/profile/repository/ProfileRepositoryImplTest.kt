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

    // --- Fakes ---

    private class FakeProfileRemoteDataSource : IProfileRemoteDataSource {
        var updateResult: Try<UserDto> = Try.failure(RuntimeException("not set"))

        override suspend fun updateProfile(name: String?, displayAlias: String?): Try<UserDto> = updateResult
    }
}
