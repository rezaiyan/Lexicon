package domain.auth.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.auth.repository.ISessionRepository

class VerifySessionUseCase(
    private val sessionRepository: ISessionRepository
) : NoParamUseCase<SessionVerificationResult> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<SessionVerificationResult> = Try {
        sessionRepository.verifySession()
    }
}

typealias SessionVerificationResult = domain.auth.repository.SessionVerificationResult
