package domain.auth.repository

import domain.auth.model.AuthUser

interface ISessionRepository {
    suspend fun verifySession(): SessionVerificationResult
}

sealed class SessionVerificationResult {
    data class Valid(val user: AuthUser) : SessionVerificationResult()
    data object Expired : SessionVerificationResult()
    data object NotAuthenticated : SessionVerificationResult()
    data object ServerError : SessionVerificationResult()
}



