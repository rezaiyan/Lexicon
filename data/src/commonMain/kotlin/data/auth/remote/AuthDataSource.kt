package data.auth.remote

import data.auth.remote.model.AppleAuthRequest
import data.auth.remote.model.AuthResponse
import data.auth.remote.model.GoogleAuthRequest
import data.auth.remote.model.RefreshTokenRequest
import data.auth.remote.model.UserDto
import data.core.network.error.AuthenticationException
import data.core.network.error.HttpErrorMapper
import data.core.network.model.ApiResponse
import core.common.Try
import core.common.doOnFailure
import core.common.map
import core.common.mapFailure
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
) : IAuthDataSource {

    suspend fun authenticateWithGoogle(idToken: String): Try<AuthResponse> {
        logNetwork("Auth", "Authenticating with Google")

        return Try {
            httpClient.post("$baseUrl/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(GoogleAuthRequest(idToken = idToken))
            }.body<ApiResponse<AuthResponse>>()
        }.map { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Authentication failed")
            }
            logNetwork("Auth", "Successfully authenticated: ${response.data.user.email}")
            response.data
        }.doOnFailure { error ->
            logNetwork("Auth", "Error in authenticateWithGoogle: ${error.message}")
        }
    }

    suspend fun authenticateWithApple(
        idToken: String,
        fullName: String? = null,
        appleUserId: String
    ): Try<AuthResponse> {
        logNetwork("Auth", "Authenticating with Apple")

        return Try {
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
        }.map { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Authentication failed")
            }
            logNetwork("Auth", "Successfully authenticated with Apple: ${response.data.user.email}")
            response.data
        }.doOnFailure { error ->
            logNetwork("Auth", "Error in authenticateWithApple: ${error.message}")
        }
    }

    suspend fun refreshTokens(refreshToken: String): Try<AuthResponse> {
        logNetwork("Auth", "Refreshing tokens")

        return Try {
            httpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }.body<ApiResponse<AuthResponse>>()
        }.map { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Token refresh failed")
            }
            logNetwork("Auth", "Successfully refreshed tokens")
            response.data
        }.doOnFailure { error ->
            logNetwork("Auth", "Error in refreshTokens: ${error.message}")
        }
    }

    suspend fun logout(refreshToken: String): Try<Unit> {
        logNetwork("Auth", "Logging out")

        return Try {
            httpClient.post("$baseUrl/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }
        }.let {
            // Best effort - always succeed even if network call fails
            if (it.isSuccess) {
                logNetwork("Auth", "Logged out successfully")
            } else {
                logNetwork("Auth", "Error in logout: ${(it as Try.Failure).throwable.message}")
            }
            Try.success(Unit)
        }
    }

    suspend fun getUserProfile(): Try<UserDto> {
        logNetwork("Auth", "Getting user profile")

        return Try {
            httpClient.get("$baseUrl/users/me")
                .body<ApiResponse<UserDto>>()
        }.map { response ->
            if (!response.success || response.data == null) {
                throw AuthenticationException(response.message ?: "Failed to get user profile")
            }
            logNetwork("Auth", "User profile retrieved: ${response.data.email}")
            response.data
        }.mapFailure { throwable ->
            val mapped = HttpErrorMapper.mapException(throwable)
            logNetwork("Auth", "Network error in getUserProfile: ${mapped.message}")
            mapped
        }
    }

    suspend fun deleteAccount(): Try<Unit> {
        logNetwork("Auth", "Deleting user account")

        return Try {
            httpClient.delete("$baseUrl/auth/delete-account")
        }.map {
            logNetwork("Auth", "Account deleted successfully")
        }.mapFailure { throwable ->
            val mapped = HttpErrorMapper.mapException(throwable)
            logNetwork("Auth", "Error in deleteAccount: ${mapped.message}")
            mapped
        }
    }
}
