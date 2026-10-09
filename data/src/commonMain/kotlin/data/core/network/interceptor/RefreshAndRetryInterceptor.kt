package data.core.network.interceptor

import data.auth.refresh.ITokenRefreshManager
import data.core.network.error.AuthenticationException
import core.common.fold
import expects.logNetwork
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

private val RETRY_KEY = AttributeKey<Boolean>("RetryKey")

/**
 * Interceptor that handles token refresh on 401/403 errors.
 * Automatically refreshes tokens and retries the original request once.
 * Only processes requests with Authorization header that are NOT auth endpoints.
 *
 * Uses HttpSend plugin so the retry response is returned to the caller.
 */
class RefreshAndRetryInterceptor(
    private val tokenRefreshManagerProvider: () -> ITokenRefreshManager
) {
    class Config {
        lateinit var tokenRefreshManagerProvider: () -> ITokenRefreshManager
    }

    companion object Plugin : HttpClientPlugin<Config, RefreshAndRetryInterceptor> {
        private val AUTH_ERROR_CODES = setOf(401, 403)

        override val key: AttributeKey<RefreshAndRetryInterceptor> =
            AttributeKey("RefreshAndRetryInterceptor")

        override fun prepare(block: Config.() -> Unit): RefreshAndRetryInterceptor {
            val config = Config().apply(block)
            return RefreshAndRetryInterceptor(config.tokenRefreshManagerProvider)
        }

        override fun install(plugin: RefreshAndRetryInterceptor, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { request ->
                // ErrorInterceptor turns a 401/403 into an AuthenticationException while the response is
                // received, i.e. inside execute(); catch it here or an expired access token would never be
                // refreshed and the user would be signed out instead.
                val originalCall = try {
                    execute(request)
                } catch (error: AuthenticationException) {
                    if (!plugin.shouldAttemptRefresh(error.statusCode, request)) throw error
                    return@intercept plugin.refreshAndRetry(request, error.statusCode) { execute(it) } ?: throw error
                }

                if (!plugin.shouldAttemptRefresh(originalCall.response.status.value, request)) {
                    return@intercept originalCall
                }
                plugin.refreshAndRetry(request, originalCall.response.status.value) { execute(it) } ?: originalCall
            }
        }
    }

    /** Refreshes the access token and resends [request] once; null when the refresh failed. */
    private suspend fun refreshAndRetry(
        request: HttpRequestBuilder,
        status: Int,
        send: suspend (HttpRequestBuilder) -> HttpClientCall,
    ): HttpClientCall? {
        logNetwork("RefreshAndRetry", "Received $status for authenticated request, attempting token refresh")
        return tokenRefreshManagerProvider().refresh().fold(
            onSuccess = { newAccessToken ->
                logNetwork("RefreshAndRetry", "Token refresh successful, retrying original request")
                request.headers.remove(HttpHeaders.Authorization)
                request.headers.append(HttpHeaders.Authorization, "Bearer $newAccessToken")
                request.attributes.put(RETRY_KEY, true)
                send(request).also {
                    logNetwork("RefreshAndRetry", "Retry completed with status ${it.response.status.value}")
                }
            },
            onFailure = { error ->
                logNetwork("RefreshAndRetry", "Token refresh failed: ${error.message}, propagating original response")
                null
            },
        )
    }

    private fun shouldAttemptRefresh(statusCode: Int, request: HttpRequestBuilder): Boolean =
        statusCode in AUTH_ERROR_CODES &&
            request.attributes.getOrNull(RETRY_KEY) != true &&
            !request.headers[HttpHeaders.Authorization].isNullOrBlank() &&
            !request.isAuthEndpoint()

    private fun HttpRequestBuilder.isAuthEndpoint(): Boolean {
        return url.build().encodedPath.lowercase().contains("/auth/")
    }
}
