package domain.notifications.usecase

import core.common.Try
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.repository.IAuthRepository
import domain.auth.usecase.IsAuthenticatedUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitializePushNotificationsUseCaseTest {

    private val authRepo = FakeAuthRepo()
    private val pushTokenRepo = FakePushTokenRepository()
    private val isAuthenticatedUseCase = IsAuthenticatedUseCase(authRepo)
    private val registerPushTokenUseCase = RegisterPushTokenUseCase(pushTokenRepo)
    private val useCase = InitializePushNotificationsUseCase(isAuthenticatedUseCase, registerPushTokenUseCase)

    @Test
    fun `registers push token when authenticated`() = runTest {
        authRepo.authenticated = true

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(pushTokenRepo.initializeAndRegisterCalled)
    }

    @Test
    fun `does not register when not authenticated`() = runTest {
        authRepo.authenticated = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertFalse(pushTokenRepo.initializeAndRegisterCalled)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        authRepo.authenticated = true

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(pushTokenRepo.initializeAndRegisterCalled)
    }

    private class FakeAuthRepo : IAuthRepository {
        var authenticated = false

        override suspend fun isAuthenticated(): Boolean = authenticated
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
