package data.auth.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class AppleAuthRequest(
    val idToken: String,
    val authorizationCode: String? = null,
    val fullName: String? = null,
    val appleUserId: String? = null
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val subscriptionStatus: String,
    val subscriptionExpiresAt: String?,
    val currentStreak: Int = 0
)

