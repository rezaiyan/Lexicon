package data.profile.repository

import data.auth.mapper.toDomain
import data.profile.remote.ProfileRemoteDataSource
import domain.auth.model.AuthUser
import domain.common.Try
import domain.common.map
import domain.profile.repository.IProfileRepository

class ProfileRepositoryImpl(
    private val remoteDataSource: ProfileRemoteDataSource
) : IProfileRepository {

    override suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser> =
        remoteDataSource.updateProfile(name, displayAlias).map { it.toDomain() }

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String> =
        remoteDataSource.uploadAvatar(imageBytes, mimeType)

    override suspend fun deleteAvatar(): Try<Unit> =
        remoteDataSource.deleteAvatar()
}
