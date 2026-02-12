package domain.notifications.usecase

import domain.notifications.repository.IPushTokenRepository

class RegisterPushTokenUseCase(
    private val pushTokenRepository: IPushTokenRepository
) {
    fun initializeAndRegister() {
        pushTokenRepository.initializeAndRegister()
    }
    
    suspend fun registerToken(token: String): Result<Unit> {
        return pushTokenRepository.registerToken(token)
    }
    
    suspend fun deactivateAllTokens(): Result<Unit> {
        return pushTokenRepository.deactivateAllTokens()
    }
}

