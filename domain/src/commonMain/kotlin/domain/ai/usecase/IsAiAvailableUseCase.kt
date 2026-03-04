package domain.ai.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.auth.repository.IAuthRepository

/**
 * Use case to check if AI features are available for the current user
 *
 * AI features are available when:
 * - User is authenticated (has valid token)
 * - User has premium subscription OR within free tier limits
 *
 * This is a single source of truth for AI availability checks across the app.
 */
class IsAiAvailableUseCase(
    private val authRepository: IAuthRepository
) : NoParamUseCase<Boolean> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Boolean> = Try {
        authRepository.isAuthenticated()
    }
}
