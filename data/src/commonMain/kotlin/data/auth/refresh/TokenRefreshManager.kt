package data.auth.refresh

import data.auth.remote.AuthDataSource
import data.auth.state.IAuthenticationStateManager
import data.auth.token.ITokenManager
import data.core.network.error.AuthenticationException
import core.common.Try
import core.common.fold
import expects.logNetwork
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Token refresh manager with single-flight refresh pattern
 * Ensures only one refresh operation runs at a time; concurrent calls wait
 *
 * Key behaviors:
 * - On auth rejection (401/403 from refresh endpoint): clears tokens and logs out
 * - On transient errors (network timeout, server 500): returns failure WITHOUT clearing tokens,
 *   so the user's session survives connectivity blips
 * - Concurrent callers are coalesced: if another caller already refreshed while we waited on the
 *   mutex, we return the new token without making a redundant refresh call
 */
class TokenRefreshManager(
    private val tokenManager: ITokenManager,
    private val authDataSource: AuthDataSource,
    private val authenticationStateManager: IAuthenticationStateManager
) : ITokenRefreshManager {

    private val refreshMutex = Mutex()

    override suspend fun refresh(): Try<String> {
        val tokenBeforeWait = tokenManager.getAccessToken()

        return refreshMutex.withLock {
            // Check if another caller already refreshed while we waited on the mutex.
            // If the current access token differs from what we saw before waiting, the
            // token was already refreshed — just return the new one.
            val currentToken = tokenManager.getAccessToken()
            if (currentToken != null && currentToken != tokenBeforeWait) {
                logNetwork("TokenRefresh", "Token already refreshed by another caller, reusing")
                return@withLock Try.success(currentToken)
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                logNetwork("TokenRefresh", "No refresh token available")
                tokenManager.clearTokens()
                authenticationStateManager.setAuthenticated(false)
                return@withLock Try.failure(AuthenticationException("No refresh token available"))
            }

            logNetwork("TokenRefresh", "Attempting token refresh")
            val result = authDataSource.refreshTokens(refreshToken)

            result.fold(
                onSuccess = { authResponse ->
                    logNetwork("TokenRefresh", "Token refresh successful")
                    tokenManager.saveTokens(
                        accessToken = authResponse.accessToken,
                        refreshToken = authResponse.refreshToken,
                        expiresInMs = authResponse.expiresIn
                    )
                    authenticationStateManager.setAuthenticated(true)
                    Try.success(authResponse.accessToken)
                },
                onFailure = { error ->
                    logNetwork("TokenRefresh", "Token refresh failed: ${error.message}")

                    // Only clear tokens if the server explicitly rejected the refresh token
                    // (auth error). Transient errors (network timeout, server 500) should NOT
                    // destroy the user's session — the refresh token may still be valid.
                    if (error is AuthenticationException) {
                        logNetwork("TokenRefresh", "Auth rejection — clearing session")
                        tokenManager.clearTokens()
                        authenticationStateManager.setAuthenticated(false)
                    } else {
                        logNetwork("TokenRefresh", "Transient error — keeping session intact")
                    }
                    Try.failure(error)
                }
            )
        }
    }

    override suspend fun clearSession() {
        logNetwork("TokenRefresh", "Clearing session (account invalid after refresh)")
        tokenManager.clearTokens()
        authenticationStateManager.setAuthenticated(false)
    }
}
