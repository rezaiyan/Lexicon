package domain.profile.repository

import domain.auth.model.AuthUser
import domain.common.Try

interface IProfileRepository {
    suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser>
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String>
    suspend fun deleteAvatar(): Try<Unit>
}
