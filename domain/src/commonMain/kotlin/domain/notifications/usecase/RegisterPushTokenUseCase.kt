package domain.notifications.usecase

import core.common.Try
import core.common.UseCase
import domain.notifications.repository.IPushTokenRepository

class RegisterPushTokenUseCase(
    private val pushTokenRepository: IPushTokenRepository
) : UseCase<String, Unit> {

    override suspend operator fun invoke(params: String): Try<Unit> {
        return pushTokenRepository.registerToken(params)
    }

    fun initializeAndRegister() {
        pushTokenRepository.initializeAndRegister()
    }

    suspend fun deactivateAllTokens(): Try<Unit> {
        return pushTokenRepository.deactivateAllTokens()
    }
}
