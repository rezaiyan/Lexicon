package data.session.repository

import core.common.Try
import data.auth.remote.IAuthDataSource
import data.auth.remote.model.AuthResponse
import data.auth.remote.model.UserDto
import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkException
import data.core.network.error.ServerException
import data.storage.SecureStorage
import domain.auth.repository.SessionVerificationResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals

class SessionRepositoryImplTest {

    private val authDataSource = FakeAuthDataSource()
    private val secureStorage = FakeSecureStorage()

    private fun createRepo() = SessionRepositoryImpl(authDataSource, secureStorage)

    private val testUserDto = UserDto(
        id = 1L, email = "test@test.com", name = "Test",
        subscriptionStatus = "FREE", subscriptionExpiresAt = null
    )

    @Test
    fun `verifySession returns NotAuthenticated when no access token`() = runTest {
        secureStorage.storedAccessToken = null
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.NotAuthenticated>(result)
    }

    @Test
    fun `verifySession returns NotAuthenticated when access token is blank`() = runTest {
        secureStorage.storedAccessToken = "  "
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.NotAuthenticated>(result)
    }

    @Test
    fun `verifySession returns Valid with user when profile succeeds`() = runTest {
        secureStorage.storedAccessToken = "valid-token"
        authDataSource.profileResult = Try.success(testUserDto)
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.Valid>(result)
        assertEquals(1L, result.user.id)
        assertEquals("test@test.com", result.user.email)
    }

    @Test
    fun `verifySession returns Expired and clears tokens on AuthenticationException`() = runTest {
        secureStorage.storedAccessToken = "expired-token"
        authDataSource.profileResult = Try.failure(AuthenticationException("Unauthorized"))
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.Expired>(result)
        assertTrue(secureStorage.tokensCleared)
    }

    @Test
    fun `verifySession returns ServerError on ServerException`() = runTest {
        secureStorage.storedAccessToken = "valid-token"
        authDataSource.profileResult = Try.failure(ServerException("Internal server error"))
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.ServerError>(result)
    }

    @Test
    fun `verifySession returns ServerError on NetworkException`() = runTest {
        secureStorage.storedAccessToken = "valid-token"
        authDataSource.profileResult = Try.failure(NetworkException("No internet"))
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.ServerError>(result)
    }

    @Test
    fun `verifySession returns ServerError on unknown exception`() = runTest {
        secureStorage.storedAccessToken = "valid-token"
        authDataSource.profileResult = Try.failure(RuntimeException("Unknown"))
        val repo = createRepo()

        val result = repo.verifySession()

        assertIs<SessionVerificationResult.ServerError>(result)
    }

    private fun assertTrue(value: Boolean) {
        kotlin.test.assertTrue(value)
    }

    // --- Fakes ---

    private class FakeAuthDataSource : IAuthDataSource {
        var profileResult: Try<UserDto> = Try.failure(RuntimeException("not set"))

        override suspend fun authenticateWithGoogle(idToken: String): Try<AuthResponse> =
            Try.failure(RuntimeException("not impl"))

        override suspend fun authenticateWithApple(
            idToken: String,
            fullName: String?,
            appleUserId: String,
        ): Try<AuthResponse> = Try.failure(RuntimeException("not impl"))

        override suspend fun refreshTokens(refreshToken: String): Try<AuthResponse> =
            Try.failure(RuntimeException("not impl"))
        override suspend fun logout(refreshToken: String): Try<Unit> = Try.success(Unit)
        override suspend fun getUserProfile(): Try<UserDto> = profileResult
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
    }

    private class FakeSecureStorage : SecureStorage {
        var storedAccessToken: String? = null
        var tokensCleared = false

        override suspend fun saveAccessToken(token: String) { storedAccessToken = token }
        override suspend fun saveRefreshToken(token: String) {}
        override fun getAccessToken(): String? = storedAccessToken
        override suspend fun getRefreshToken(): String? = null
        override suspend fun clearTokens() { tokensCleared = true; storedAccessToken = null }
        override suspend fun saveTokenExpiresAt(expiresAtMs: Long) {}
        override fun getTokenExpiresAt(): Long = 0L
        override suspend fun hasCompletedOnboarding(): Boolean = false
        override suspend fun markOnboardingCompleted() {}
    }
}
