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

class LoginWithAppleUseCaseTest {

    private val authService = FakeAuthService()
    private val useCase = LoginWithAppleUseCase(authService)

    @Test
    fun `login emits auth user on success`() = runTest {
        val expected = AuthUser(1L, "apple@test.com", "Apple User")
        authService.appleUser = expected

        val result = useCase(LoginWithAppleUseCase.Params("apple-token", "Full Name", "apple-user-id")).first()

        assertEquals(expected, result)
    }

    @Test
    fun `login passes params to auth service`() = runTest {
        authService.appleUser = AuthUser(1L, "test@test.com", "Test")

        useCase.invoke("my-token", "My Name", "my-apple-id").first()

        assertEquals("my-token", authService.lastAppleIdToken)
        assertEquals("My Name", authService.lastAppleFullName)
        assertEquals("my-apple-id", authService.lastAppleUserId)
    }

    @Test
    fun `login with null name passes null to auth service`() = runTest {
        authService.appleUser = AuthUser(1L, "test@test.com", "Test")

        useCase.invoke("token", null, "apple-id").first()

        assertEquals(null, authService.lastAppleFullName)
    }

    @Test
    fun `login throws when auth service fails`() = runTest {
        authService.shouldThrow = true

        assertFailsWith<RuntimeException> {
            useCase(LoginWithAppleUseCase.Params("token", null, "id")).first()
        }
    }

    private class FakeAuthService : IAuthenticationService {
        var appleUser: AuthUser = AuthUser(1L, "test@test.com", "Test")
        var lastAppleIdToken: String? = null
        var lastAppleFullName: String? = null
        var lastAppleUserId: String? = null
        var shouldThrow = false

        override fun loginWithGoogle(idToken: String): Flow<AuthUser> = flowOf()
        override fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<AuthUser> {
            lastAppleIdToken = idToken
            lastAppleFullName = fullName
            lastAppleUserId = appleUserId
            if (shouldThrow) return flow { throw RuntimeException("Apple login failed") }
            return flowOf(appleUser)
        }
        override fun logout(): Flow<Unit> = flowOf(Unit)
        override fun deleteAccount(): Flow<Unit> = flowOf(Unit)
    }
}
