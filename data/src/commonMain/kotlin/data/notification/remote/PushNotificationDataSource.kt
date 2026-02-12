package data.notification.remote

import data.core.network.model.ApiResponse
import data.notification.remote.model.RegisterPushTokenRequest
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
) {

    /**
     * Register FCM push token with the backend
     */
    suspend fun registerPushToken(request: RegisterPushTokenRequest): Result<Unit> {
        if (getAuthToken() == null) {
            logNetwork("PushNotification", "Cannot register token - user not authenticated")
            return Result.failure(Exception("User not authenticated"))
        }

        logNetwork("PushNotification", "Registering push token for platform: ${request.platform}")

        return runCatching {
            httpClient.post("$baseUrl/notifications/register-token") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ApiResponse<Unit>>()
        }.mapCatching { response ->
            if (!response.success) {
                throw Exception(response.message ?: "Failed to register push token")
            }
            logNetwork("PushNotification", "Push token registered successfully")
            Unit
        }.onFailure { error ->
            logNetwork("PushNotification", "Error registering push token: ${error.message}")
        }
    }

    /**
     * Deactivate all push tokens for the current user (useful for logout)
     */
    suspend fun deactivateAllTokens(): Result<Unit> {
        if (getAuthToken() == null) {
            return Result.success(Unit)
        }

        logNetwork("PushNotification", "Deactivating all push tokens")

        val result = runCatching {
            httpClient.delete("$baseUrl/notifications/tokens")
        }

        return result.fold(
            onSuccess = {
                logNetwork("PushNotification", "Push tokens deactivated")
                Result.success(Unit)
            },
            onFailure = { error ->
                logNetwork("PushNotification", "Error deactivating tokens: ${error.message}")
                // Best effort cleanup – still report success to callers
                Result.success(Unit)
            }
        )
    }
}

