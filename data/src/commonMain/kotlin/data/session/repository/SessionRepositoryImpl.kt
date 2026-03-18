package data.session.repository

import analytics.IAnalyticsTracker
import data.auth.mapper.toDomain
import data.auth.remote.IAuthDataSource
import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkException
import data.core.network.error.ServerException
import data.storage.SecureStorage
import domain.auth.repository.ISessionRepository
import domain.auth.repository.SessionVerificationResult
import core.common.fold

/**
 * Session repository that verifies sessions using backend tokens only
 * Token refresh on expiry is handled automatically by RefreshAndRetryInterceptor
 */
class SessionRepositoryImpl(
    private val authDataSource: IAuthDataSource,
    private val secureStorage: SecureStorage,
    private val analyticsTracker: IAnalyticsTracker
) : ISessionRepository {

    override suspend fun verifySession(): SessionVerificationResult {
        val accessToken = secureStorage.getAccessToken()

        if (accessToken.isNullOrBlank()) {
            return SessionVerificationResult.NotAuthenticated
        }

        val result = authDataSource.getUserProfile()

        return result.fold(
            onSuccess = { userDto ->
                val user = userDto.toDomain()
                SessionVerificationResult.Valid(user)
            },
            onFailure = { error ->
                when (error) {
                    is AuthenticationException -> {
                        analyticsTracker.logEvent(
                            "auto_logout",
                            mapOf("reason" to "session_expired", "source" to "session_repository")
                        )
                        secureStorage.clearTokens()
                        SessionVerificationResult.Expired
                    }

                    is ServerException,
                    is NetworkException -> {
                        SessionVerificationResult.ServerError
                    }

                    else -> {
                        SessionVerificationResult.ServerError
                    }
                }
            }
        )
    }

}

