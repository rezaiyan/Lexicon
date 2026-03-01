package presentation.model


data class ProfileUserUiModel(
    val name: String,
    val email: String,
    val displayAlias: String? = null,
    val profileImageUrl: String? = null
)

