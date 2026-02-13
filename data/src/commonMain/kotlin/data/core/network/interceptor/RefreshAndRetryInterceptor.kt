package data.core.network.interceptor

import data.auth.refresh.ITokenRefreshManager
import data.core.network.error.AuthenticationException
import domain.common.Try
import domain.common.fold
import expects.logNetwork
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
import io.ktor.util.AttributeKey
import io.ktor.utils.io.cancel

private val RETRY_KEY = AttributeKey<Boolean>("RetryKey")
private val REQUEST_BODY_KEY = AttributeKey<Any>("RequestBodyKey")

/**
 * Interceptor that handles token refresh on 401/403 errors
 * Automatically refreshes tokens and retries the original request once
 * Only processes requests with Authorization header that are NOT auth endpoints
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
            AttributeKey<RefreshAndRetryInterceptor>("RefreshAndRetryInterceptor")

        override fun prepare(block: Config.() -> Unit): RefreshAndRetryInterceptor {
            val config = Config().apply(block)
            return RefreshAndRetryInterceptor(config.tokenRefreshManagerProvider)
        }

        override fun install(plugin: RefreshAndRetryInterceptor, scope: HttpClient) {
            // Store request body before sending for potential retry
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val body = context.body
                context.attributes.put(REQUEST_BODY_KEY, body)
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.After) {
                val call = context
                val container = subject
                val response = container.response as? HttpResponse ?: run {
                    proceedWith(container)
                    return@intercept
                }

                val request = call.request
                if (!plugin.shouldAttemptRefresh(response, request)) {
                    proceedWith(container)
                    return@intercept
                }

                logNetwork(
                    "RefreshAndRetry",
                    "Received ${response.status.value} for authenticated request, attempting token refresh"
                )

                response.bodyAsChannel().cancel()

                val refreshResult = plugin.tokenRefreshManagerProvider().refresh()
                refreshResult.fold(
                    onSuccess = { newAccessToken ->
                        logNetwork(
                            "RefreshAndRetry",
                            "Token refresh successful, retrying original request"
                        )

                        val retryResponse: HttpResponse
                        try {
                            retryResponse = plugin.executeRetry(
                                client = call.client,
                                originalCall = call,
                                newAccessToken = newAccessToken
                            )
                        } catch (e: AuthenticationException) {
                            // Refresh succeeded but retry still rejected (401/403).
                            // The account itself is invalid (deleted, banned, etc.).
                            logNetwork(
                                "RefreshAndRetry",
                                "Retry rejected after successful refresh — clearing session"
                            )
                            Try { plugin.tokenRefreshManagerProvider().clearSession() }
                            throw e
                        }

                        logNetwork(
                            "RefreshAndRetry",
                            "Retry completed with status ${retryResponse.status.value}"
                        )

                        val newContainer = HttpResponseContainer(
                            container.expectedType,
                            retryResponse
                        )
                        proceedWith(newContainer)
                    },
                    onFailure = { error ->
                        logNetwork(
                            "RefreshAndRetry",
                            "Token refresh failed: ${error.message}, propagating original response"
                        )
                        proceedWith(container)
                    }
                )
            }
        }
    }

    private suspend fun executeRetry(
        client: HttpClient,
        originalCall: HttpClientCall,
        newAccessToken: String
    ): HttpResponse {
        val originalRequest = originalCall.request

        return client.request {
            url.takeFrom(originalRequest.url)
            method = originalRequest.method
            copyHeadersFromRequest(originalRequest)
            headers.remove(HttpHeaders.Authorization)
            headers.append(HttpHeaders.Authorization, "Bearer $newAccessToken")
            copyBodyFromRequest(originalCall, originalRequest.method)
            markAsRetry()
        }
    }

    private fun HttpRequestBuilder.copyHeadersFromRequest(
        originalRequest: HttpRequest
    ) {
        originalRequest.headers.forEach { name, values ->
            values.forEach { value ->
                headers.append(name, value)
            }
        }
    }

    private fun HttpRequestBuilder.copyBodyFromRequest(
        originalCall: HttpClientCall,
        originalMethod: HttpMethod
    ) {
        if (originalMethod.supportsRequestBody()) {
            val storedBody = originalCall.request.attributes.getOrNull(REQUEST_BODY_KEY)
            if (storedBody != null) {
                setBody(storedBody)
            }
        }
    }

    private fun HttpRequestBuilder.markAsRetry() {
        attributes.put(RETRY_KEY, true)
    }

    private fun shouldAttemptRefresh(
        response: HttpResponse,
        request: HttpRequest
    ): Boolean {
        if (response.status.value !in AUTH_ERROR_CODES) return false
        if (request.attributes.getOrNull(RETRY_KEY) == true) return false
        if (request.headers[HttpHeaders.Authorization].isNullOrBlank()) return false
        if (request.isAuthEndpoint()) return false
        return true
    }

    private fun HttpRequest.isAuthEndpoint(): Boolean {
        val path = url.encodedPath.lowercase()
        return path.contains("/auth/")
    }

    private fun HttpMethod.supportsRequestBody(): Boolean = when (this) {
        HttpMethod.Get,
        HttpMethod.Head,
        HttpMethod.Options -> false

        else -> true
    }

}
