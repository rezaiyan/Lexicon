package domain.auth.usecase

import domain.auth.service.IAuthenticationService
import domain.common.Try
import domain.common.fold
import domain.settings.repository.ISettingsRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteAccountUseCase(
    private val authenticationService: IAuthenticationService,
    private val wordRepository: IWordRepository,
    private val settingsRepository: ISettingsRepository
) {
    fun invoke(): Flow<Try<Unit>> = flow {
        // Perform account deletion on server first
        authenticationService.deleteAccount().collect { result ->
            result.fold(
                onSuccess = {
                    // Clear all local data after successful account deletion
                    wordRepository.deleteAllWords()
                    settingsRepository.clearSettings()
                    settingsRepository.clearInsightData()
                    emit(Try.success(Unit))
                },
                onFailure = { error ->
                    emit(Try.failure(error))
                }
            )
        }
    }
}
