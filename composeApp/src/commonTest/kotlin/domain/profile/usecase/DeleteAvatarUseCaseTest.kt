package domain.profile.usecase

import core.common.Try
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeleteAvatarUseCaseTest {

    private val repository = FakeProfileRepository()
    private val useCase = DeleteAvatarUseCase(repository)

    @Test
    fun `deletes avatar successfully`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure on repository error`() = runTest {
        repository.deleteAvatarResult = Try.failure(RuntimeException("Delete failed"))

        val result = useCase()

        assertTrue(result.isFailure)
    }
}
