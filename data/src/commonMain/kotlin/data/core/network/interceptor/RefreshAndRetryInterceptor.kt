package data.core.network.interceptor

import data.auth.refresh.ITokenRefreshManager
import core.common.fold
import expects.logNetwork
import io.ktor.client.HttpClient
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
                val originalCall = execute(request)

                if (!plugin.shouldAttemptRefresh(originalCall.response.status.value, request)) {
                    return@intercept originalCall
                }

                logNetwork(
                    "RefreshAndRetry",
                    "Received ${originalCall.response.status.value} for authenticated request, attempting token refresh"
                )

                val refreshResult = plugin.tokenRefreshManagerProvider().refresh()
                refreshResult.fold(
                    onSuccess = { newAccessToken ->
                        logNetwork("RefreshAndRetry", "Token refresh successful, retrying original request")
                        request.headers.remove(HttpHeaders.Authorization)
                        request.headers.append(HttpHeaders.Authorization, "Bearer $newAccessToken")
                        request.attributes.put(RETRY_KEY, true)
                        val retryCall = execute(request)
                        logNetwork("RefreshAndRetry", "Retry completed with status ${retryCall.response.status.value}")
                        retryCall
                    },
                    onFailure = { error ->
                        logNetwork(
                            "RefreshAndRetry",
                            "Token refresh failed: ${error.message}, propagating original response"
                        )
                        originalCall
                    }
                )
            }
        }
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
