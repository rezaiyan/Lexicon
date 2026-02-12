package domain.auth.usecase

import domain.auth.repository.ISessionRepository

class VerifySessionUseCase(
    private val sessionRepository: ISessionRepository
) {
    suspend operator fun invoke(): SessionVerificationResult {
        return sessionRepository.verifySession()
    }
}

typealias SessionVerificationResult = domain.auth.repository.SessionVerificationResult

