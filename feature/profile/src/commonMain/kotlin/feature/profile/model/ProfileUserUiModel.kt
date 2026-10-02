package feature.profile.model


data class ProfileUserUiModel(
    val name: String,
    val email: String,
    val displayAlias: String? = null
)
