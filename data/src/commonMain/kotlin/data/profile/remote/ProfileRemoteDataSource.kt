package data.profile.remote

import data.auth.remote.model.UserDto
import data.core.network.client.ApiClient
import data.profile.remote.model.UpdateProfileRequestDto
import core.common.Try
import core.common.doOnSuccess
import expects.logNetwork

class ProfileRemoteDataSource(
    private val apiClient: ApiClient
) : IProfileRemoteDataSource {

    override suspend fun updateProfile(name: String?, displayAlias: String?): Try<UserDto> =
        apiClient.patchNotNull<UserDto>(
            "/users/me",
            UpdateProfileRequestDto(name = name, displayAlias = displayAlias)
        ).doOnSuccess { response ->
            logNetwork("ProfileRemoteDataSource", "Profile updated: ${response.email}")
        }
}
