package domain.notifications.usecase

import core.common.Try
import domain.notifications.repository.IPushTokenRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterPushTokenUseCaseTest {

    private val repository = FakePushTokenRepository()
    private val useCase = RegisterPushTokenUseCase(repository)

    @Test
    fun `registers token successfully`() = runTest {
        val result = useCase("fcm-token-123")

        assertTrue(result.isSuccess)
        assertEquals("fcm-token-123", repository.lastRegisteredToken)
    }

    @Test
    fun `returns failure when registration fails`() = runTest {
        repository.registerResult = Try.failure(RuntimeException("Registration failed"))

        val result = useCase("token")

        assertTrue(result.isFailure)
    }

    @Test
    fun `initializeAndRegister delegates to repository`() {
        useCase.initializeAndRegister()

        assertTrue(repository.initializeAndRegisterCalled)
    }
}

internal class FakePushTokenRepository : IPushTokenRepository {
    var lastRegisteredToken: String? = null
    var registerResult: Try<Unit> = Try.success(Unit)
    var deactivateResult: Try<Unit> = Try.success(Unit)
    var deactivateCurrentResult: Try<Unit> = Try.success(Unit)
    var deactivateAllCalled = false
    var deactivateCurrentCalled = false
    var initializeAndRegisterCalled = false

    override suspend fun registerToken(token: String): Try<Unit> {
        lastRegisteredToken = token
        return registerResult
    }

    override suspend fun deactivateAllTokens(): Try<Unit> {
        deactivateAllCalled = true
        return deactivateResult
    }

    override suspend fun deactivateCurrentToken(): Try<Unit> {
        deactivateCurrentCalled = true
        return deactivateCurrentResult
    }

    override fun initializeAndRegister() {
        initializeAndRegisterCalled = true
    }
}
