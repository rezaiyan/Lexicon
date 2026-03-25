package fakes

import core.common.Try
import domain.notifications.repository.IPushTokenRepository

class FakePushTokenRepository : IPushTokenRepository {
    var lastRegisteredToken: String? = null
    var registerResult: Try<Unit> = Try.success(Unit)
    var deactivateResult: Try<Unit> = Try.success(Unit)
    var deactivateAllCalled = false
    var initializeAndRegisterCalled = false

    override suspend fun registerToken(token: String): Try<Unit> {
        lastRegisteredToken = token
        return registerResult
    }

    override suspend fun deactivateAllTokens(): Try<Unit> {
        deactivateAllCalled = true
        return deactivateResult
    }

    override fun initializeAndRegister() {
        initializeAndRegisterCalled = true
    }
}
