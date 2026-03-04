package domain.notifications.usecase

import core.common.Try
import domain.notifications.repository.IPushTokenRepository

class RegisterPushTokenUseCase(
    private val pushTokenRepository: IPushTokenRepository
) {
    fun initializeAndRegister() {
        pushTokenRepository.initializeAndRegister()
    }

    suspend fun registerToken(token: String): Try<Unit> {
        return pushTokenRepository.registerToken(token)
    }

    suspend fun deactivateAllTokens(): Try<Unit> {
        return pushTokenRepository.deactivateAllTokens()
    }
}
