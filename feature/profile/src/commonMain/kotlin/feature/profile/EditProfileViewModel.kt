package feature.profile

import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import core.common.UiState
import core.common.fold
import core.error.toUserMessage
import domain.profile.model.AliasValidationResult
import domain.profile.usecase.DeleteAvatarUseCase
import domain.profile.usecase.UpdateProfileUseCase
import domain.profile.usecase.UploadAvatarUseCase
import domain.profile.usecase.ValidateDisplayAliasUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import core.base.BaseViewModel

data class EditProfileState(
    val displayAlias: String = "",
    val profileImageUrl: String? = null,
    val name: String = "",
    val email: String = "",
    val saveState: UiState<Unit> = UiState.Loaded(Unit),
    val isUploadingAvatar: Boolean = false,
)

sealed interface EditProfileEffect {
    data object ProfileSaved : EditProfileEffect
}

class EditProfileViewModel(
    private val userManager: IUserManager,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val deleteAvatarUseCase: DeleteAvatarUseCase,
    private val validateDisplayAliasUseCase: ValidateDisplayAliasUseCase,
) : BaseViewModel<EditProfileState, EditProfileEffect>() {

    override fun initialState() = EditProfileState()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userManager.observeUser().first() ?: return@launch
            updateState {
                copy(
                    displayAlias = user.displayAlias ?: "",
                    profileImageUrl = user.profileImageUrl,
                    name = user.name,
                    email = user.email
                )
            }
        }
    }

    fun updateDisplayAlias(value: String) {
        updateState { copy(displayAlias = value, saveState = UiState.Loaded(Unit)) }
    }

    fun saveProfile() {
        if (currentState.saveState is UiState.Loading) return

        val alias = currentState.displayAlias.trim()

        when (validateDisplayAliasUseCase(alias)) {
            AliasValidationResult.TooShort -> {
                updateState { copy(saveState = UiState.Error("Username must be 2-30 characters")) }
                return
            }
            AliasValidationResult.TooLong -> {
                updateState { copy(saveState = UiState.Error("Username must be 2-30 characters")) }
                return
            }
            AliasValidationResult.InvalidCharacters -> {
                updateState { copy(saveState = UiState.Error("Only letters, numbers, spaces, underscores, and hyphens allowed")) }
                return
            }
            AliasValidationResult.Valid -> Unit
        }

        viewModelScope.launch {
            updateState { copy(saveState = UiState.Loading) }

            val aliasToSend = alias.ifEmpty { null }
            updateProfileUseCase(name = null, displayAlias = aliasToSend).fold(
                onSuccess = { updatedUser ->
                    userManager.setUser(updatedUser)
                    updateState { copy(saveState = UiState.Loaded(Unit)) }
                    emitEffect(EditProfileEffect.ProfileSaved)
                },
                onFailure = { error ->
                    updateState { copy(saveState = UiState.Error(error.toUserMessage())) }
                }
            )
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            updateState { copy(isUploadingAvatar = true) }

            uploadAvatarUseCase(imageBytes, mimeType).fold(
                onSuccess = { url ->
                    updateState { copy(isUploadingAvatar = false, profileImageUrl = url) }
                    val currentUser = userManager.observeUser().first()
                    if (currentUser != null) {
                        userManager.setUser(currentUser.copy(profileImageUrl = url))
                    }
                },
                onFailure = { error ->
                    updateState {
                        copy(
                            isUploadingAvatar = false,
                            saveState = UiState.Error(error.toUserMessage()),
                        )
                    }
                }
            )
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            updateState { copy(isUploadingAvatar = true) }

            deleteAvatarUseCase().fold(
                onSuccess = {
                    updateState { copy(isUploadingAvatar = false, profileImageUrl = null) }
                    val currentUser = userManager.observeUser().first()
                    if (currentUser != null) {
                        userManager.setUser(currentUser.copy(profileImageUrl = null))
                    }
                },
                onFailure = { error ->
                    updateState {
                        copy(
                            isUploadingAvatar = false,
                            saveState = UiState.Error(error.toUserMessage()),
                        )
                    }
                }
            )
        }
    }
}
