package data.profile.remote

import data.auth.remote.model.UserDto
import data.core.network.client.ApiClient
import data.core.network.model.ApiResponse
import data.profile.remote.model.AvatarResponseDto
import data.profile.remote.model.UpdateProfileRequestDto
import core.common.Try
import core.common.doOnSuccess
import core.common.flatMap
import expects.logNetwork
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class ProfileRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun updateProfile(name: String?, displayAlias: String?): Try<UserDto> =
        apiClient.patchNotNull<UserDto>(
            "/users/me",
            UpdateProfileRequestDto(name = name, displayAlias = displayAlias)
        ).doOnSuccess { response ->
            logNetwork("ProfileRemoteDataSource", "Profile updated: ${response.email}")
        }

    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Try<String> {
        val extension = mimeType.substringAfter("/", "jpg")
        return Try {
            val response = apiClient.httpClient.submitFormWithBinaryData(
                url = "${apiClient.baseUrl}/users/me/avatar",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"avatar.$extension\""
                        )
                    })
                }
            )
            response.body<ApiResponse<AvatarResponseDto>>()
        }.flatMap { apiResponse ->
            if (apiResponse.success && apiResponse.data != null) {
                logNetwork("ProfileRemoteDataSource", "Avatar uploaded: ${apiResponse.data.profileImageUrl}")
                Try.success(apiResponse.data.profileImageUrl)
            } else {
                Try.failure(Exception(apiResponse.message ?: "Failed to upload avatar"))
            }
        }
    }

    suspend fun deleteAvatar(): Try<Unit> =
        apiClient.delete("/users/me/avatar")
            .doOnSuccess {
                logNetwork("ProfileRemoteDataSource", "Avatar deleted")
            }
}
