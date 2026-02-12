package data.core.network.interceptor

import data.core.network.error.HttpErrorMapper
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

/**
 * HTTP Interceptor that handles all HTTP errors globally
 * Maps non-2xx status codes to appropriate exceptions
 * Note: Token refresh and logout on 401/403 are handled by RefreshAndRetryInterceptor
 */
class ErrorInterceptor {
    fun createPlugin(): ClientPlugin<Unit> = createClientPlugin("ErrorInterceptor") {
        onResponse { response ->
            val statusCode = response.status.value

            if (statusCode !in 200..299) {
                throw HttpErrorMapper.mapHttpResponse(response)
            }
        }
    }
}

