package data.profile.remote

import data.auth.remote.model.UserDto
import core.common.Try

interface IProfileRemoteDataSource {
    suspend fun updateProfile(name: String?, displayAlias: String?): Try<UserDto>
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String>
    suspend fun deleteAvatar(): Try<Unit>
}
