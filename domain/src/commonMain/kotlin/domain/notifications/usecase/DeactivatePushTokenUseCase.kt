package domain.notifications.usecase

import core.common.Try
import domain.notifications.repository.IPushTokenRepository

class DeactivatePushTokenUseCase(
    private val pushTokenRepository: IPushTokenRepository
) {

    suspend fun deactivateCurrentToken(): Try<Unit> {
        return pushTokenRepository.deactivateCurrentToken()
    }

    suspend fun deactivateAllTokens(): Try<Unit> {
        return pushTokenRepository.deactivateAllTokens()
    }
}
