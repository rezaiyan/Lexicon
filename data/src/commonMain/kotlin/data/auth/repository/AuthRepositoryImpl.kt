package data.auth.repository

import auth.IAppleAuthStateProvider
import auth.IGoogleAuthStateProvider
import data.auth.mapper.toDomain
import data.auth.remote.AuthDataSource
import data.auth.remote.FeatureAccessRemoteDataSource
import data.auth.session.ISessionManager
import data.auth.token.ITokenManager
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class AuthRepositoryImpl(
    private val tokenManager: ITokenManager,
    private val sessionManager: ISessionManager,
    private val featureAccessRemoteDataSource: FeatureAccessRemoteDataSource,
    private val authDataSource: AuthDataSource,
    private val googleAuthStateProvider: IGoogleAuthStateProvider,
    private val appleAuthStateProvider: IAppleAuthStateProvider
) : IAuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        return performLogin {
            authDataSource.authenticateWithGoogle(idToken)
        }
    }

    override suspend fun loginWithApple(
        idToken: String,
        fullName: String?,
        appleUserId: String
    ): Result<AuthUser> {
        return performLogin {
            authDataSource.authenticateWithApple(idToken, fullName, appleUserId)
        }
    }

    private suspend fun performLogin(
        authenticate: suspend () -> Result<data.auth.remote.model.AuthResponse>
    ): Result<AuthUser> {
        val authResult = authenticate()

        return authResult.fold(
            onSuccess = { authResponse ->
                tokenManager.saveTokens(
                    authResponse.accessToken,
                    authResponse.refreshToken
                )
                sessionManager.setAuthenticated(true)
                val user = authResponse.user.toDomain()
                Result.success(user)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun logout(): Result<Unit> {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken != null) {
            authDataSource.logout(refreshToken)
        }

        googleAuthStateProvider.signOutFromGoogle()
        appleAuthStateProvider.signOutFromApple()

        tokenManager.clearTokens()
        sessionManager.setAuthenticated(false)

        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val accessToken = tokenManager.getAccessToken()

        if (accessToken == null) {
            return Result.failure(Exception("Not authenticated"))
        }

        val deleteResult = authDataSource.deleteAccount()

        if (deleteResult.isFailure) {
            return deleteResult
        }

        googleAuthStateProvider.signOutFromGoogle()
        appleAuthStateProvider.signOutFromApple()

        tokenManager.clearTokens()
        sessionManager.setAuthenticated(false)

        return Result.success(Unit)
    }

    override suspend fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

    override suspend fun isAuthenticated(): Boolean {
        return sessionManager.isAuthenticated()
    }

    override fun isAuthenticatedAsFlow(): Flow<Boolean> {
        return sessionManager.isAuthenticatedFlow
    }

    override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> {
        return sessionManager.isAuthenticatedFlow
            .flatMapLatest { isAuthenticated ->
                if (isAuthenticated) {
                    featureAccessRemoteDataSource.getFeatureAccessAsFlow()
                        .catch { error ->
                            emit(defaultFeatureAccess())
                        }
                } else {
                    flowOf(defaultFeatureAccess())
                }
            }
            .catch { error ->
                emit(defaultFeatureAccess())
            }
    }

    private fun defaultFeatureAccess(): FeatureAccessResponse {
        return FeatureAccessResponse(
            featureFlags = FeatureFlags(
                premiumFeaturesEnabled = false,
                aiImageExtractionEnabled = false,
                aiDailyInsightEnabled = false,
                pushNotificationsEnabled = true,
                subscriptionsEnabled = false
            ),
            userAccess = UserFeatureAccess(
                hasPremiumAccess = false,
                canUseAiImageExtraction = false,
                canUseAiDailyInsight = false,
                subscriptionStatus = "FREE",
                subscriptionExpiresAt = null,
                aiExtractionUsageCount = 0,
                aiExtractionUsageLimit = 10,
                remainingAiExtractions = 10
            )
        )
    }
}

