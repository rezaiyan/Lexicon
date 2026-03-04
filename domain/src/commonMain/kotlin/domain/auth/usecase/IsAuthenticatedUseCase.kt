package domain.auth.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for checking if user is authenticated
 */
class IsAuthenticatedUseCase(
    private val authRepository: IAuthRepository
) : NoParamUseCase<Boolean> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Boolean> = Try {
        authRepository.isAuthenticated()
    }

    fun asFlow(): Flow<Boolean> {
        return authRepository.isAuthenticatedAsFlow()
    }
}
