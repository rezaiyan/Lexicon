package domain.auth.usecase

import core.common.NoParamFlowUseCase
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Reactive stream of the user's authentication state.
 * Emits `true` when authenticated, `false` when not.
 *
 * Prefer this over [IsAuthenticatedUseCase.asFlow] — the latter is an
 * anti-pattern that leaks the repository boundary into the use-case interface.
 */
class ObserveAuthStateUseCase(
    private val authRepository: IAuthRepository,
) : NoParamFlowUseCase<Boolean> {

    override operator fun invoke(params: Unit): Flow<Boolean> =
        authRepository.isAuthenticatedAsFlow()
}
