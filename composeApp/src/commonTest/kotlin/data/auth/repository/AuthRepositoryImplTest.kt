package data.auth.repository

import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import core.common.Try
import core.common.getOrThrow
import data.auth.remote.IAuthDataSource
import data.auth.remote.IFeatureAccessRemoteDataSource
import data.auth.remote.model.AuthResponse
import data.auth.remote.model.UserDto
import data.auth.token.ITokenManager
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.session.ISessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthRepositoryImplTest {

    private val tokenManager = FakeTokenManager()
    private val sessionManager = FakeSessionManager()
    private val featureAccessDataSource = FakeFeatureAccessDataSource()
    private val authDataSource = FakeAuthDataSource()
    private val googleAuthProvider = FakeGoogleAuthProvider()
    private val appleAuthProvider = FakeAppleAuthProvider()

    private fun createRepo() = AuthRepositoryImpl(
        tokenManager = tokenManager,
        sessionManager = sessionManager,
        featureAccessRemoteDataSource = featureAccessDataSource,
        authDataSource = authDataSource,
        googleAuthStateProvider = googleAuthProvider,
        appleAuthStateProvider = appleAuthProvider
    )

    private val testUserDto = UserDto(
        id = 1L, email = "test@test.com", name = "Test",
        subscriptionStatus = "FREE", subscriptionExpiresAt = null
    )

    private val testAuthResponse = AuthResponse(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        tokenType = "Bearer",
        expiresIn = 3600L,
        user = testUserDto
    )

    // --- loginWithGoogle ---

    @Test
    fun `loginWithGoogle saves tokens on success`() = runTest {
        authDataSource.googleResult = Try.success(testAuthResponse)
        val repo = createRepo()

        val result = repo.loginWithGoogle("id-token")

        assertTrue(result.isSuccess)
        assertEquals("access-token", tokenManager.savedAccessToken)
        assertEquals("refresh-token", tokenManager.savedRefreshToken)
    }

    @Test
    fun `loginWithGoogle sets authenticated on success`() = runTest {
        authDataSource.googleResult = Try.success(testAuthResponse)
        val repo = createRepo()

        repo.loginWithGoogle("id-token")

        assertTrue(sessionManager.authenticated)
    }

    @Test
    fun `loginWithGoogle returns mapped AuthUser on success`() = runTest {
        authDataSource.googleResult = Try.success(testAuthResponse)
        val repo = createRepo()

        val result = repo.loginWithGoogle("id-token")

        val user = result.getOrThrow()
        assertEquals(1L, user.id)
        assertEquals("test@test.com", user.email)
        assertEquals("Test", user.name)
    }

    @Test
    fun `loginWithGoogle returns failure when auth fails`() = runTest {
        authDataSource.googleResult = Try.failure(RuntimeException("Auth failed"))
        val repo = createRepo()

        val result = repo.loginWithGoogle("id-token")

        assertTrue(result.isFailure)
    }

    // --- loginWithApple ---

    @Test
    fun `loginWithApple saves tokens on success`() = runTest {
        authDataSource.appleResult = Try.success(testAuthResponse)
        val repo = createRepo()

        val result = repo.loginWithApple("id-token", "Test User", "apple-123")

        assertTrue(result.isSuccess)
        assertEquals("access-token", tokenManager.savedAccessToken)
    }

    @Test
    fun `loginWithApple returns failure when auth fails`() = runTest {
        authDataSource.appleResult = Try.failure(RuntimeException("Apple auth failed"))
        val repo = createRepo()

        val result = repo.loginWithApple("id-token", null, "apple-123")

        assertTrue(result.isFailure)
    }

    // --- logout ---

    @Test
    fun `logout clears tokens and sets unauthenticated`() = runTest {
        tokenManager.savedRefreshToken = "refresh-token"
        val repo = createRepo()

        val result = repo.logout()

        assertTrue(result.isSuccess)
        assertTrue(tokenManager.tokensCleared)
        assertFalse(sessionManager.authenticated)
    }

    @Test
    fun `logout signs out from Google and Apple`() = runTest {
        val repo = createRepo()

        repo.logout()

        assertTrue(googleAuthProvider.signedOut)
        assertTrue(appleAuthProvider.signedOut)
    }

    @Test
    fun `logout calls backend logout with refresh token`() = runTest {
        tokenManager.savedRefreshToken = "my-refresh"
        val repo = createRepo()

        repo.logout()

        assertEquals("my-refresh", authDataSource.lastLogoutRefreshToken)
    }

    // --- deleteAccount ---

    @Test
    fun `deleteAccount returns failure when not authenticated`() = runTest {
        tokenManager.savedAccessToken = null
        val repo = createRepo()

        val result = repo.deleteAccount()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteAccount clears tokens on success`() = runTest {
        tokenManager.savedAccessToken = "token"
        authDataSource.deleteResult = Try.success(Unit)
        val repo = createRepo()

        val result = repo.deleteAccount()

        assertTrue(result.isSuccess)
        assertTrue(tokenManager.tokensCleared)
        assertFalse(sessionManager.authenticated)
    }

    @Test
    fun `deleteAccount returns failure when backend fails`() = runTest {
        tokenManager.savedAccessToken = "token"
        authDataSource.deleteResult = Try.failure(RuntimeException("Server error"))
        val repo = createRepo()

        val result = repo.deleteAccount()

        assertTrue(result.isFailure)
    }

    // --- getAccessToken / isAuthenticated ---

    @Test
    fun `getAccessToken delegates to token manager`() = runTest {
        tokenManager.savedAccessToken = "my-token"
        val repo = createRepo()

        assertEquals("my-token", repo.getAccessToken())
    }

    @Test
    fun `isAuthenticated delegates to session manager`() = runTest {
        sessionManager.authenticated = true
        val repo = createRepo()

        assertTrue(repo.isAuthenticated())
    }

    // --- getFeatureAccessAsFlow ---

    @Test
    fun `getFeatureAccessAsFlow returns default when not authenticated`() = runTest {
        sessionManager.authFlow.value = false
        val repo = createRepo()

        val result = repo.getFeatureAccessAsFlow().first()

        assertFalse(result.userAccess.hasPremiumAccess)
    }

    // --- Fakes ---

    private class FakeTokenManager : ITokenManager {
        var savedAccessToken: String? = null
        var savedRefreshToken: String? = null
        var tokensCleared = false

        override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresInMs: Long) {
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }
        override suspend fun getAccessToken(): String? = savedAccessToken
        override suspend fun getRefreshToken(): String? = savedRefreshToken
        override suspend fun clearTokens() { tokensCleared = true; savedAccessToken = null; savedRefreshToken = null }
        override suspend fun hasTokens(): Boolean = savedAccessToken != null
        override fun getTokenExpiresAt(): Long = 0L
    }

    private class FakeSessionManager : ISessionManager {
        var authenticated = false
        val authFlow = MutableStateFlow(false)
        override val isAuthenticatedFlow: StateFlow<Boolean> get() = authFlow
        override suspend fun setAuthenticated(isAuthenticated: Boolean) {
            authenticated = isAuthenticated
            authFlow.value = isAuthenticated
        }
        override suspend fun isAuthenticated(): Boolean = authenticated
        override fun initialize(scope: CoroutineScope) {}
    }

    private class FakeFeatureAccessDataSource : IFeatureAccessRemoteDataSource {
        var featureAccess = FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = false))
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf(featureAccess)
    }

    private class FakeAuthDataSource : IAuthDataSource {
        var googleResult: Try<AuthResponse> = Try.failure(RuntimeException("not set"))
        var appleResult: Try<AuthResponse> = Try.failure(RuntimeException("not set"))
        var deleteResult: Try<Unit> = Try.success(Unit)
        var lastLogoutRefreshToken: String? = null

        override suspend fun authenticateWithGoogle(idToken: String): Try<AuthResponse> =
            googleResult

        override suspend fun authenticateWithApple(
            idToken: String,
            fullName: String?,
            appleUserId: String,
        ): Try<AuthResponse> = appleResult

        override suspend fun refreshTokens(refreshToken: String): Try<AuthResponse> =
            Try.failure(RuntimeException("not impl"))

        override suspend fun logout(refreshToken: String): Try<Unit> {
            lastLogoutRefreshToken = refreshToken
            return Try.success(Unit)
        }
        override suspend fun getUserProfile(): Try<UserDto> = Try.failure(RuntimeException("not impl"))
        override suspend fun deleteAccount(): Try<Unit> = deleteResult
    }

    private class FakeGoogleAuthProvider : IGoogleAuthStateProvider {
        var signedOut = false
        override fun isSignedInWithGoogle(): Boolean = false
        override suspend fun getSilentGoogleIdToken(): String? = null
        override suspend fun signOutFromGoogle() { signedOut = true }
    }

    private class FakeAppleAuthProvider : IAppleAuthStateProvider {
        var signedOut = false
        override suspend fun isSignedInWithApple(): Boolean = false
        override suspend fun getAppleUserIdentifier(): String? = null
        override suspend fun signOutFromApple() { signedOut = true }
    }
}
