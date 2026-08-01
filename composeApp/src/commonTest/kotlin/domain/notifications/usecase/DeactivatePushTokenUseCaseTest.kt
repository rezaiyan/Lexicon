package domain.notifications.usecase

import core.common.Try
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeactivatePushTokenUseCaseTest {

    private val repository = FakePushTokenRepository()
    private val useCase = DeactivatePushTokenUseCase(repository)

    @Test
    fun `deactivateAllTokens delegates to repository`() = runTest {
        val result = useCase.deactivateAllTokens()

        assertTrue(result.isSuccess)
        assertTrue(repository.deactivateAllCalled)
    }

    @Test
    fun `deactivateAllTokens returns failure on error`() = runTest {
        repository.deactivateResult = Try.failure(RuntimeException("Deactivation failed"))

        val result = useCase.deactivateAllTokens()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deactivateCurrentToken delegates to repository`() = runTest {
        val result = useCase.deactivateCurrentToken()

        assertTrue(result.isSuccess)
        assertTrue(repository.deactivateCurrentCalled)
    }

    @Test
    fun `deactivateCurrentToken returns failure on error`() = runTest {
        repository.deactivateCurrentResult = Try.failure(RuntimeException("Deactivation failed"))

        val result = useCase.deactivateCurrentToken()

        assertTrue(result.isFailure)
    }
}
