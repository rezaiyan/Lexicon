package domain.auth.usecase

import domain.auth.service.IAuthenticationService
import domain.common.Try
import domain.common.fold
import domain.settings.repository.ISettingsRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LogoutUseCase(
    private val authenticationService: IAuthenticationService,
    private val wordRepository: IWordRepository,
    private val settingsRepository: ISettingsRepository
) {
    fun invoke(): Flow<Try<Unit>> = flow {
        // Clear all user data first
        wordRepository.deleteAllWords()
        settingsRepository.clearSettings()
        settingsRepository.clearInsightData()

        // Then perform logout (which clears tokens)
        authenticationService.logout().collect { result ->
            result.fold(
                onSuccess = {
                    emit(Try.success(Unit))
                },
                onFailure = { _ ->
                    // Even if logout API call fails, data is cleared
                    emit(Try.success(Unit))
                }
            )
        }
    }
}
