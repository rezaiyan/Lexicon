package data.auth.remote

import data.auth.remote.model.AuthResponse
import data.auth.remote.model.UserDto
import core.common.Try

interface IAuthDataSource {
    suspend fun authenticateWithGoogle(idToken: String): Try<AuthResponse>
    suspend fun authenticateWithApple(
        idToken: String,
        fullName: String?,
        appleUserId: String
    ): Try<AuthResponse>
    /** Verifies a passkey sign-in response; see `PasskeyRemoteDataSource` for its options. */
    suspend fun authenticateWithPasskey(responseJson: String): Try<AuthResponse>
    suspend fun refreshTokens(refreshToken: String): Try<AuthResponse>
    suspend fun logout(refreshToken: String): Try<Unit>
    suspend fun getUserProfile(): Try<UserDto>
    suspend fun deleteAccount(): Try<Unit>
}
