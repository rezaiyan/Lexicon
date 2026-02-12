package domain.auth.usecase

import domain.auth.model.FeatureAccessResponse
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to get feature access information.
 * Returns user's feature flags and access status.
 */
class GetFeatureAccessUseCase(
    private val authRepository: IAuthRepository
) {
    operator fun invoke(): Flow<FeatureAccessResponse> {
        return authRepository.getFeatureAccessAsFlow()
    }
}
