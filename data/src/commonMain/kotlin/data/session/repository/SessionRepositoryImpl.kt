package data.session.repository

import data.auth.remote.AuthDataSource
import data.auth.remote.model.UserDto
import data.core.network.error.AuthenticationException
import data.core.network.error.NetworkException
import data.core.network.error.ServerException
import data.storage.SecureStorage
import domain.auth.model.AuthUser
import domain.auth.model.SubscriptionStatus
import domain.auth.repository.ISessionRepository
import domain.auth.repository.SessionVerificationResult
import domain.common.fold

/**
 * Session repository that verifies sessions using backend tokens only
 * Token refresh on expiry is handled automatically by RefreshAndRetryInterceptor
 */
class SessionRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val secureStorage: SecureStorage
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

    private fun UserDto.toDomain(): AuthUser {
        return AuthUser(
            id = this.id,
            email = this.email,
            name = this.name,
            profileImageUrl = this.profileImageUrl,
            subscriptionStatus = SubscriptionStatus.valueOf(this.subscriptionStatus),
            subscriptionExpiresAt = this.subscriptionExpiresAt,
            currentStreak = this.currentStreak,
            longestStreak = this.longestStreak
        )
    }
}

