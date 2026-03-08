package domain.ai.usecase

import core.common.Try
import core.common.getOrThrow
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsAiAvailableUseCaseTest {

    private val authRepo = FakeAuthRepo()
    private val useCase = IsAiAvailableUseCase(authRepo)

    @Test
    fun `returns true when user is authenticated`() = runTest {
        authRepo.authenticated = true

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `returns false when user is not authenticated`() = runTest {
        authRepo.authenticated = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        authRepo.authenticated = true

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        authRepo.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }

    private class FakeAuthRepo : IAuthRepository {
        var authenticated = false
        var shouldThrow = false

        override suspend fun isAuthenticated(): Boolean {
            if (shouldThrow) throw RuntimeException("Auth check failed")
            return authenticated
        }

        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(authenticated)
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = Try.success(AuthUser(1, "", ""))
        override suspend fun loginWithApple(
            idToken: String,
            fullName: String?,
            appleUserId: String,
        ): Try<AuthUser> = Try.success(AuthUser(1, "", ""))
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String? = null
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf()
    }
}
