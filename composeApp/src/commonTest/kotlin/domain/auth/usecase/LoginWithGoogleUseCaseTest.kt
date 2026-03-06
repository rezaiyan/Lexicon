package domain.auth.usecase

import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LoginWithGoogleUseCaseTest {

    private val authService = FakeAuthService()
    private val useCase = LoginWithGoogleUseCase(authService)

    @Test
    fun `login emits auth user on success`() = runTest {
        val expected = AuthUser(1L, "test@test.com", "Test User")
        authService.googleUser = expected

        val result = useCase("google-id-token").first()

        assertEquals(expected, result)
    }

    @Test
    fun `login passes id token to auth service`() = runTest {
        authService.googleUser = AuthUser(1L, "test@test.com", "Test")

        useCase("my-token-123").first()

        assertEquals("my-token-123", authService.lastGoogleIdToken)
    }

    @Test
    fun `login throws when auth service fails`() = runTest {
        authService.shouldThrow = true

        assertFailsWith<RuntimeException> {
            useCase("token").first()
        }
    }

    private class FakeAuthService : IAuthenticationService {
        var googleUser: AuthUser = AuthUser(1L, "test@test.com", "Test")
        var lastGoogleIdToken: String? = null
        var shouldThrow = false

        override fun loginWithGoogle(idToken: String): Flow<AuthUser> {
            lastGoogleIdToken = idToken
            if (shouldThrow) return flow { throw RuntimeException("Login failed") }
            return flowOf(googleUser)
        }

        override fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<AuthUser> = flowOf()
        override fun logout(): Flow<Unit> = flowOf(Unit)
        override fun deleteAccount(): Flow<Unit> = flowOf(Unit)
    }
}
