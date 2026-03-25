package fakes

import domain.auth.repository.ISessionRepository
import domain.auth.repository.SessionVerificationResult

class FakeSessionRepository : ISessionRepository {
    var result: SessionVerificationResult = SessionVerificationResult.NotAuthenticated
    var shouldThrow = false

    override suspend fun verifySession(): SessionVerificationResult {
        if (shouldThrow) throw RuntimeException("Session verification failed")
        return result
    }
}
