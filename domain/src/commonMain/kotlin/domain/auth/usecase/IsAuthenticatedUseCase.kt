package domain.auth.usecase

import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for checking if user is authenticated
 */
class IsAuthenticatedUseCase(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isAuthenticated()
    }

    fun asFlow(): Flow<Boolean> {
        return authRepository.isAuthenticatedAsFlow()
    }
}


