package data.core.network.interceptor

import data.auth.refresh.ITokenRefreshManager
import data.auth.token.ITokenManager
import core.common.fold
import expects.logNetwork
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import kotlin.time.Clock

/**
 * Interceptor that adds Bearer token to requests and proactively refreshes
 * the access token before it expires, so the user never hits a 401.
 */
class AuthInterceptor(
    private val tokenManager: ITokenManager,
    private val tokenRefreshManagerProvider: (() -> ITokenRefreshManager)? = null
) {
    companion object {
        /**
         * Refresh the token when it's within 5 minutes of expiring.
         * For a 24-hour token, this means proactive refresh at ~99.6% of lifetime.
         */
        private const val PROACTIVE_REFRESH_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes

        /**
         * Public endpoints that don't require authentication.
         * Mirrors the backend's SecurityConfig permitAll() list.
         * All other endpoints get the Bearer token automatically.
         */
        private val PUBLIC_PATHS = listOf(
            "/auth/google",
            "/auth/apple",
            "/auth/refresh",
        )
    }

    private fun isPublicEndpoint(path: String): Boolean =
        PUBLIC_PATHS.any { path.contains(it) }

    fun createPlugin(): ClientPlugin<Unit> = createClientPlugin("AuthInterceptor") {
        onRequest { request, _ ->
            val path = request.url.encodedPath.lowercase()

            if (isPublicEndpoint(path)) return@onRequest

            // Check if token is about to expire and proactively refresh
            val expiresAt = tokenManager.getTokenExpiresAt()
            if (expiresAt > 0 && tokenRefreshManagerProvider != null) {
                val now = Clock.System.now().toEpochMilliseconds()
                val timeUntilExpiry = expiresAt - now

                if (timeUntilExpiry in 1..PROACTIVE_REFRESH_THRESHOLD_MS) {
                    logNetwork("AuthInterceptor", "Token expires in ${timeUntilExpiry / 1000}s, proactively refreshing")
                    try {
                        val refreshResult = tokenRefreshManagerProvider.invoke().refresh()
                        refreshResult.fold(
                            onSuccess = {
                                logNetwork("AuthInterceptor", "Proactive refresh successful")
                            },
                            onFailure = { error ->
                                logNetwork("AuthInterceptor", "Proactive refresh failed: ${error.message}, using current token")
                            }
                        )
                    } catch (e: Exception) {
                        logNetwork("AuthInterceptor", "Proactive refresh error: ${e.message}")
                    }
                }
            }

            // Add the (possibly refreshed) token
            val token = tokenManager.getAccessToken()
            if (token != null && !request.headers.contains(HttpHeaders.Authorization)) {
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}