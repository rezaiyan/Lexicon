package domain.auth.usecase

import core.common.NoParamFlowUseCase
import domain.auth.service.IAuthenticationService
import domain.settings.repository.ISettingsRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class LogoutUseCase(
    private val authenticationService: IAuthenticationService,
    private val wordRepository: IWordRepository,
    private val settingsRepository: ISettingsRepository
) : NoParamFlowUseCase<Unit> {

    override operator fun invoke(params: Unit): Flow<Unit> = invoke()

    fun invoke(): Flow<Unit> = flow {
        // Clear all user data first
        wordRepository.deleteAllWords()
        settingsRepository.clearSettings()
        settingsRepository.clearInsightData()

        // Then perform logout (which clears tokens)
        authenticationService.logout()
            .catch {
                // Even if logout API call fails, data is already cleared
                emit(Unit)
            }
            .collect {
                emit(Unit)
            }
    }
}
