package domain.auth.usecase

import core.common.NoParamFlowUseCase
import core.common.getOrThrow
import domain.auth.service.IAuthenticationService
import domain.settings.repository.ISettingsRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteAccountUseCase(
    private val authenticationService: IAuthenticationService,
    private val wordRepository: IWordRepository,
    private val settingsRepository: ISettingsRepository
) : NoParamFlowUseCase<Unit> {

    override operator fun invoke(params: Unit): Flow<Unit> = invoke()

    fun invoke(): Flow<Unit> = flow {
        authenticationService.deleteAccount().collect {
            // Clear all local data after successful account deletion
            wordRepository.deleteAllWords().getOrThrow()
            settingsRepository.clearSettings().getOrThrow()
            emit(Unit)
        }
    }
}
