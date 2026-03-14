package domain.auth.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.getOrThrow
import domain.auth.session.ISessionManager
import domain.auth.storage.ISecureStorage
import domain.settings.repository.ISettingsRepository
import domain.word.repository.IWordRepository

/**
 * Use case to clear all user data from the device.
 * Used when user signs out or when account is deleted remotely.
 */
class ClearAllUserDataUseCase(
    private val wordRepository: IWordRepository,
    private val settingsRepository: ISettingsRepository,
    private val secureStorage: ISecureStorage,
    private val sessionManager: ISessionManager
) : NoParamUseCase<Unit> {
    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Unit> = Try {
        wordRepository.deleteAllWords().getOrThrow()
        settingsRepository.clearSettings().getOrThrow()
        secureStorage.clearTokens()
        sessionManager.setAuthenticated(false)
    }
}
