package domain.auth.service

import core.common.Try
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthenticationServiceTest {

    private val repository = FakeAuthRepo()
    private val service = AuthenticationService(repository)

    @Test
    fun `loginWithGoogle emits user on success`() = runTest {
        val expected = AuthUser(1L, "test@test.com", "Test User")
        repository.loginResult = Try.success(expected)

        val result = service.loginWithGoogle("google-token").first()

        assertEquals(expected, result)
    }

    @Test
    fun `loginWithGoogle throws on failure`() = runTest {
        repository.loginResult = Try.failure(RuntimeException("Login failed"))

        assertFailsWith<RuntimeException> {
            service.loginWithGoogle("token").first()
        }
    }

    @Test
    fun `loginWithApple emits user on success`() = runTest {
        val expected = AuthUser(2L, "apple@test.com", "Apple User")
        repository.loginResult = Try.success(expected)

        val result = service.loginWithApple("apple-token", "Full Name", "apple-id").first()

        assertEquals(expected, result)
    }

    @Test
    fun `loginWithApple throws on failure`() = runTest {
        repository.loginResult = Try.failure(RuntimeException("Apple login failed"))

        assertFailsWith<RuntimeException> {
            service.loginWithApple("token", null, "id").first()
        }
    }

    @Test
    fun `logout emits Unit on success`() = runTest {
        repository.logoutResult = Try.success(Unit)

        val result = service.logout().first()

        assertEquals(Unit, result)
    }

    @Test
    fun `logout throws on failure`() = runTest {
        repository.logoutResult = Try.failure(RuntimeException("Logout failed"))

        assertFailsWith<RuntimeException> {
            service.logout().first()
        }
    }

    @Test
    fun `deleteAccount emits Unit on success`() = runTest {
        repository.deleteResult = Try.success(Unit)

        val result = service.deleteAccount().first()

        assertEquals(Unit, result)
    }

    @Test
    fun `deleteAccount throws on failure`() = runTest {
        repository.deleteResult = Try.failure(RuntimeException("Delete failed"))

        assertFailsWith<RuntimeException> {
            service.deleteAccount().first()
        }
    }

    private class FakeAuthRepo : IAuthRepository {
        var loginResult: Try<AuthUser> = Try.success(AuthUser(1L, "test@test.com", "Test"))
        var logoutResult: Try<Unit> = Try.success(Unit)
        var deleteResult: Try<Unit> = Try.success(Unit)

        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = loginResult
        override suspend fun loginWithApple(
            idToken: String,
            fullName: String?,
            appleUserId: String,
        ): Try<AuthUser> = loginResult
        override suspend fun logout(): Try<Unit> = logoutResult
        override suspend fun deleteAccount(): Try<Unit> = deleteResult
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = false
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(false)
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf()
    }
}
