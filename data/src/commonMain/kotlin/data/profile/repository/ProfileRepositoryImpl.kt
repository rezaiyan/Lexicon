package data.profile.repository

import data.auth.mapper.toDomain
import data.profile.remote.IProfileRemoteDataSource
import domain.auth.model.AuthUser
import core.common.Try
import core.common.map
import domain.profile.repository.IProfileRepository

class ProfileRepositoryImpl(
    private val remoteDataSource: IProfileRemoteDataSource
) : IProfileRepository {

    override suspend fun updateProfile(name: String?, displayAlias: String?): Try<AuthUser> =
        remoteDataSource.updateProfile(name, displayAlias).map { it.toDomain() }
}
