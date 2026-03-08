package domain.profile.usecase

import core.common.Try
import core.common.getOrThrow
import domain.auth.model.AuthUser
import domain.profile.repository.IProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateProfileUseCaseTest {

    private val repository = FakeProfileRepository()
    private val useCase = UpdateProfileUseCase(repository)

    @Test
    fun `updates profile with name and alias`() = runTest {
        val expected = AuthUser(1L, "test@test.com", "New Name", displayAlias = "NewAlias")
        repository.updateResult = Try.success(expected)

        val result = useCase("New Name", "NewAlias")

        assertTrue(result.isSuccess)
        assertEquals("New Name", result.getOrThrow().name)
        assertEquals("NewAlias", result.getOrThrow().displayAlias)
    }

    @Test
    fun `updates profile with null name`() = runTest {
        repository.updateResult = Try.success(AuthUser(1L, "test@test.com", "Test"))

        val result = useCase(null, "Alias")

        assertTrue(result.isSuccess)
        assertEquals(null, repository.lastUpdateName)
        assertEquals("Alias", repository.lastUpdateAlias)
    }

    @Test
    fun `updates profile with Params`() = runTest {
        repository.updateResult = Try.success(AuthUser(1L, "test@test.com", "Test"))

        val result = useCase(UpdateProfileUseCase.Params("Name", "Alias"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure on repository error`() = runTest {
        repository.updateResult = Try.failure(RuntimeException("Update failed"))

        val result = useCase("Name", "Alias")

        assertTrue(result.isFailure)
    }
}

internal class FakeProfileRepository : IProfileRepository {
    var updateResult: Try<AuthUser> = Try.success(AuthUser(1L, "test@test.com", "Test"))
    var uploadResult: Try<String> = Try.success("https://avatar.url")
    var deleteAvatarResult: Try<Unit> = Try.success(Unit)
    var lastUpdateName: String? = null
    var lastUpdateAlias: String? = null

    override suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser> {
        lastUpdateName = name
        lastUpdateAlias = displayAlias
        return updateResult
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String> = uploadResult
    override suspend fun deleteAvatar(): Try<Unit> = deleteAvatarResult
}
