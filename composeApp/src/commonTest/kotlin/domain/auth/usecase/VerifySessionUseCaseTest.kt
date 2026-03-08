package domain.auth.usecase

import core.common.getOrThrow
import domain.auth.model.AuthUser
import domain.auth.repository.ISessionRepository
import domain.auth.repository.SessionVerificationResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerifySessionUseCaseTest {

    private val repository = FakeSessionRepository()
    private val useCase = VerifySessionUseCase(repository)

    @Test
    fun `returns Valid when session is valid`() = runTest {
        val user = AuthUser(1L, "test@test.com", "Test")
        repository.result = SessionVerificationResult.Valid(user)

        val result = useCase()

        assertTrue(result.isSuccess)
        val verification = result.getOrThrow()
        assertTrue(verification is SessionVerificationResult.Valid)
        assertEquals(user, (verification as SessionVerificationResult.Valid).user)
    }

    @Test
    fun `returns Expired when session is expired`() = runTest {
        repository.result = SessionVerificationResult.Expired

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() is SessionVerificationResult.Expired)
    }

    @Test
    fun `returns NotAuthenticated when not authenticated`() = runTest {
        repository.result = SessionVerificationResult.NotAuthenticated

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() is SessionVerificationResult.NotAuthenticated)
    }

    @Test
    fun `returns ServerError when server fails`() = runTest {
        repository.result = SessionVerificationResult.ServerError

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() is SessionVerificationResult.ServerError)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        repository.result = SessionVerificationResult.Expired

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() is SessionVerificationResult.Expired)
    }

    @Test
    fun `repository exception returns failure`() = runTest {
        repository.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }
}

private class FakeSessionRepository : ISessionRepository {
    var result: SessionVerificationResult = SessionVerificationResult.NotAuthenticated
    var shouldThrow = false

    override suspend fun verifySession(): SessionVerificationResult {
        if (shouldThrow) throw RuntimeException("Session verification failed")
        return result
    }
}
