package data.auth.remote

import data.auth.remote.model.AppleAuthRequest
import data.auth.remote.model.AuthResponse
import data.auth.remote.model.GoogleAuthRequest
import data.auth.remote.model.RefreshTokenRequest
import data.auth.remote.model.UserDto
import data.core.network.error.AuthenticationException
import data.core.network.error.HttpErrorMapper
import data.core.network.model.ApiResponse
import expects.logNetwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Data source for authentication endpoints
 */
class AuthDataSource(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {

    suspend fun authenticateWithGoogle(idToken: String): Result<AuthResponse> {
        logNetwork("Auth", "Authenticating with Google")

        return runCatching {
            httpClient.post("$baseUrl/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(GoogleAuthRequest(idToken = idToken))
            }.body<ApiResponse<AuthResponse>>()
        }.mapCatching { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Authentication failed")
            }
            logNetwork("Auth", "Successfully authenticated: ${response.data.user.email}")
            response.data
        }.onFailure { error ->
            logNetwork("Auth", "Error in authenticateWithGoogle: ${error.message}")
        }
    }

    suspend fun authenticateWithApple(
        idToken: String,
        fullName: String? = null,
        appleUserId: String
    ): Result<AuthResponse> {
        logNetwork("Auth", "Authenticating with Apple")

        return runCatching {
            httpClient.post("$baseUrl/auth/apple") {
                contentType(ContentType.Application.Json)
                setBody(
                    AppleAuthRequest(
                        idToken = idToken,
                        fullName = fullName,
                        appleUserId = appleUserId
                    )
                )
            }.body<ApiResponse<AuthResponse>>()
        }.mapCatching { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Authentication failed")
            }
            logNetwork("Auth", "Successfully authenticated with Apple: ${response.data.user.email}")
            response.data
        }.onFailure { error ->
            logNetwork("Auth", "Error in authenticateWithApple: ${error.message}")
        }
    }

    suspend fun refreshTokens(refreshToken: String): Result<AuthResponse> {
        logNetwork("Auth", "Refreshing tokens")

        return runCatching {
            httpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }.body<ApiResponse<AuthResponse>>()
        }.mapCatching { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Token refresh failed")
            }
            logNetwork("Auth", "Successfully refreshed tokens")
            response.data
        }.onFailure { error ->
            logNetwork("Auth", "Error in refreshTokens: ${error.message}")
        }
    }

    suspend fun logout(refreshToken: String): Result<Unit> {
        logNetwork("Auth", "Logging out")

        val result = runCatching {
            httpClient.post("$baseUrl/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }
        }

        return result.fold(
            onSuccess = {
                logNetwork("Auth", "Logged out successfully")
                Result.success(Unit)
            },
            onFailure = { error ->
                logNetwork("Auth", "Error in logout: ${error.message}")
                Result.success(Unit)
            }
        )
    }

    suspend fun getUserProfile(): Result<UserDto> {
        logNetwork("Auth", "Getting user profile")

        return runCatching {
            httpClient.get("$baseUrl/users/me")
                .body<ApiResponse<UserDto>>()
        }.mapCatching { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Failed to get user profile")
            }
            logNetwork("Auth", "User profile retrieved: ${response.data.email}")
            response.data
        }.recoverCatching { throwable ->
            throw HttpErrorMapper.mapException(throwable)
        }.onFailure { error ->
            logNetwork("Auth", "Network error in getUserProfile: ${error.message}")
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        logNetwork("Auth", "Deleting user account")

        return runCatching {
            httpClient.delete("$baseUrl/auth/delete-account")
        }.map {
            logNetwork("Auth", "Account deleted successfully")
            Unit
        }.recoverCatching { throwable ->
            throw HttpErrorMapper.mapException(throwable)
        }.onFailure { error ->
            logNetwork("Auth", "Error in deleteAccount: ${error.message}")
        }
    }
}

