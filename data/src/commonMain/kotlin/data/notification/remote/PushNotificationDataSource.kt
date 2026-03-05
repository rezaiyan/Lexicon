package data.notification.remote

import data.core.network.model.ApiResponse
import data.notification.remote.model.RegisterPushTokenRequest
import core.common.Try
import core.common.doOnFailure
import core.common.map
import expects.logNetwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
     * Deactivate all push tokens for the current user (useful for logout)
     */
    override suspend fun deactivateAllTokens(): Try<Unit> {
        if (getAuthToken() == null) {
            return Try.success(Unit)
        }

        logNetwork("PushNotification", "Deactivating all push tokens")

        return Try {
            httpClient.delete("$baseUrl/notifications/tokens")
        }.let {
            // Best effort cleanup – still report success to callers
            if (it.isSuccess) {
                logNetwork("PushNotification", "Push tokens deactivated")
            } else {
                logNetwork("PushNotification", "Error deactivating tokens: ${(it as Try.Failure).throwable.message}")
            }
            Try.success(Unit)
        }
    }
}
