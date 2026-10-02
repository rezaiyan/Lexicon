package data.profile.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequestDto(
    val name: String? = null,
    val displayAlias: String? = null
)
