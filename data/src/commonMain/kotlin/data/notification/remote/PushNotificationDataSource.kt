package data.notification.remote

import data.core.network.model.ApiResponse
import data.notification.remote.model.RegisterPushTokenRequest
import core.common.Try
import core.common.doOnFailure
import core.common.map
import core.common.onFailure
import core.common.onSuccess
import expects.logNetwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType

/**
 * Data source for push notification endpoints
 */
class PushNotificationDataSource(
    private val baseUrl: String,
    private val getAuthToken: suspend () -> String?,
    private val httpClient: HttpClient
) : IPushNotificationDataSource {

    /**
     * Register FCM push token with the backend
     */
    override suspend fun registerPushToken(request: RegisterPushTokenRequest): Try<Unit> {
        if (getAuthToken() == null) {
            logNetwork("PushNotification", "Cannot register token - user not authenticated")
            return Try.failure(Exception("User not authenticated"))
        }

        logNetwork("PushNotification", "Registering push token for platform: ${request.platform}")

        return Try {
            httpClient.post("$baseUrl/notifications/register-token") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ApiResponse<Unit>>()
        }.map { response ->
            if (!response.success) {
                throw Exception(response.message ?: "Failed to register push token")
            }
            logNetwork("PushNotification", "Push token registered successfully")
        }.doOnFailure { error ->
            logNetwork("PushNotification", "Error registering push token: ${error.message}")
        }
    }

    /**
     * Deactivate all push tokens for the current user (useful for account deletion)
     */
    override suspend fun deactivateAllTokens(): Try<Unit> {
        return deactivate(logLabel = "all push tokens") {
            httpClient.delete("$baseUrl/notifications/tokens")
        }
    }

    /**
     * Deactivate a single push token (the current device's), e.g. on single-device logout
     */
    override suspend fun deactivateToken(token: String): Try<Unit> {
        return deactivate(logLabel = "push token") {
            httpClient.delete("$baseUrl/notifications/token") {
                url { appendPathSegments(token) }
            }
        }
    }

    /**
     * Best-effort token deactivation: any outcome (success, HTTP error, or being
     * unauthenticated) resolves to Try.success(Unit) — callers cannot act on a
     * server-side dereg failure anyway, so we just log it.
     */
    private suspend fun deactivate(logLabel: String, request: suspend () -> Unit): Try<Unit> {
        if (getAuthToken() == null) {
            return Try.success(Unit)
        }

        logNetwork("PushNotification", "Deactivating $logLabel")

        Try { request() }
            .onSuccess { logNetwork("PushNotification", "Deactivated $logLabel") }
            .onFailure { error -> logNetwork("PushNotification", "Error deactivating $logLabel: ${error.message}") }

        return Try.success(Unit)
    }
}
