package domain.auth.usecase

import core.common.Try
import core.common.getOrThrow
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsAuthenticatedUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = IsAuthenticatedUseCase(repository)

    @Test
    fun `returns true when user is authenticated`() = runTest {
        repository.authenticated = true

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `returns false when user is not authenticated`() = runTest {
        repository.authenticated = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        repository.authenticated = true

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `asFlow emits authentication state`() = runTest {
        repository.authenticatedFlow = flowOf(true)

        val result = useCase.asFlow().first()

        assertTrue(result)
    }

    @Test
    fun `asFlow emits false when not authenticated`() = runTest {
        repository.authenticatedFlow = flowOf(false)

        val result = useCase.asFlow().first()

        assertFalse(result)
    }

    @Test
    fun `repository exception returns failure`() = runTest {
        repository.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }
}

internal class FakeAuthRepository : IAuthRepository {
    var authenticated = false
    var authenticatedFlow: Flow<Boolean> = flowOf(false)
    var shouldThrow = false
    var loginCallCount = 0
    var logoutCallCount = 0
    var deleteAccountCallCount = 0

    override suspend fun isAuthenticated(): Boolean {
        if (shouldThrow) throw RuntimeException("Auth check failed")
        return authenticated
    }

    override fun isAuthenticatedAsFlow(): Flow<Boolean> = authenticatedFlow

    override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> {
        loginCallCount++
        return Try.success(AuthUser(1L, "test@test.com", "Test"))
    }

    override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> {
        loginCallCount++
        return Try.success(AuthUser(1L, "test@test.com", "Test"))
    }

    override suspend fun logout(): Try<Unit> {
        logoutCallCount++
        return Try.success(Unit)
    }

    override suspend fun deleteAccount(): Try<Unit> {
        deleteAccountCallCount++
        return Try.success(Unit)
    }

    override suspend fun getAccessToken(): String? = "fake-token"

    var featureAccessFlow: Flow<FeatureAccessResponse> = flowOf()
    override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = featureAccessFlow
}
