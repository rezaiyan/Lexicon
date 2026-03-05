package presentation.feature.profile

import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import core.common.fold
import domain.profile.usecase.DeleteAvatarUseCase
import domain.profile.usecase.UpdateProfileUseCase
import domain.profile.usecase.UploadAvatarUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import presentation.base.BaseViewModel

data class EditProfileState(
    val displayAlias: String = "",
    val profileImageUrl: String? = null,
    val name: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val errorMessage: String? = null
)

sealed interface EditProfileEffect {
    data object ProfileSaved : EditProfileEffect
}

class EditProfileViewModel(
    private val userManager: IUserManager,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val deleteAvatarUseCase: DeleteAvatarUseCase
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
        updateState { copy(displayAlias = value) }
    }

    fun dismissError() {
        updateState { copy(errorMessage = null) }
    }

    fun saveProfile() {
        val alias = currentState.displayAlias.trim()

        if (alias.isNotEmpty() && (alias.length < 2 || alias.length > 30)) {
            updateState { copy(errorMessage = "Username must be 2-30 characters") }
            return
        }

        if (alias.isNotEmpty() && !alias.matches("^[a-zA-Z0-9 _-]+$".toRegex())) {
            updateState { copy(errorMessage = "Only letters, numbers, spaces, underscores, and hyphens allowed") }
            return
        }

        viewModelScope.launch {
            updateState { copy(isSaving = true, errorMessage = null) }

            val aliasToSend = alias.ifEmpty { null }
            updateProfileUseCase(name = null, displayAlias = aliasToSend).fold(
                onSuccess = { updatedUser ->
                    userManager.setUser(updatedUser)
                    updateState { copy(isSaving = false) }
                    emitEffect(EditProfileEffect.ProfileSaved)
                },
                onFailure = { error ->
                    updateState {
                        copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to update profile"
                        )
                    }
                }
            )
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            updateState { copy(isUploadingAvatar = true, errorMessage = null) }

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
                            errorMessage = error.message ?: "Failed to upload avatar"
                        )
                    }
                }
            )
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            updateState { copy(isUploadingAvatar = true, errorMessage = null) }

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
                            errorMessage = error.message ?: "Failed to delete avatar"
                        )
                    }
                }
            )
        }
    }
}
