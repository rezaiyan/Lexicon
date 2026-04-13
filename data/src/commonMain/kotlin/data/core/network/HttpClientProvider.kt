package data.core.network

import data.auth.refresh.ITokenRefreshManager
import data.core.network.interceptor.AuthInterceptor
import data.core.network.interceptor.ErrorInterceptor
import data.core.network.interceptor.LoggingTimingInterceptor
import data.core.network.interceptor.PlatformHeaderInterceptor
import data.core.network.interceptor.RefreshAndRetryInterceptor
import core.isDebugMode
import expects.logNetwork
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HttpClient provider for the application
 * Centralizes HttpClient configuration and ensures consistent behavior across all data sources
 */
object HttpClientProvider {

    /**
     * Creates a configured HttpClient instance with interceptors
     * This configuration is shared across all data sources to ensure consistency
     *
     * @param authInterceptor Optional auth interceptor for adding Authorization headers
     * @param tokenRefreshManagerProvider Optional provider for token refresh manager
     * @param errorInterceptor Optional error interceptor for handling HTTP errors
     */
    fun createHttpClient(
        authInterceptor: AuthInterceptor? = null,
        tokenRefreshManagerProvider: (() -> ITokenRefreshManager)? = null,
        errorInterceptor: ErrorInterceptor? = null
    ): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        logNetwork("HttpClient", message)
                    }
                }
                level = if (isDebugMode()) LogLevel.HEADERS else LogLevel.NONE
            }

            // Logging + timing: always installed first so it wraps all other interceptors
            install(LoggingTimingInterceptor)

            // Platform header: appended to every outgoing request
            install(PlatformHeaderInterceptor)

            // Install interceptors if provided
            // Order matters: AuthInterceptor first, then RefreshAndRetryInterceptor, then ErrorInterceptor
            authInterceptor?.let { install(it.createPlugin()) }
            tokenRefreshManagerProvider?.let { provider ->
                install(RefreshAndRetryInterceptor.Plugin) {
                    this.tokenRefreshManagerProvider = provider
                }
            }
            errorInterceptor?.let { install(it.createPlugin()) }
        }
    }
}