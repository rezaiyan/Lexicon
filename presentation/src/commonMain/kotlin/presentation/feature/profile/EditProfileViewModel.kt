package presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import domain.common.fold
import domain.profile.usecase.DeleteAvatarUseCase
import domain.profile.usecase.UpdateProfileUseCase
import domain.profile.usecase.UploadAvatarUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileState(
    val displayAlias: String = "",
    val profileImageUrl: String? = null,
    val name: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val errorMessage: String? = null
)

sealed interface EditProfileEvent {
    data class UpdateDisplayAlias(val value: String) : EditProfileEvent
    data object SaveProfile : EditProfileEvent
    data class UploadAvatar(val imageBytes: ByteArray, val mimeType: String) : EditProfileEvent
    data object DeleteAvatar : EditProfileEvent
    data object DismissError : EditProfileEvent
}

sealed interface EditProfileEffect {
    data object ProfileSaved : EditProfileEffect
}

class EditProfileViewModel(
    private val userManager: IUserManager,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val deleteAvatarUseCase: DeleteAvatarUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<EditProfileEffect>()
    val effects: SharedFlow<EditProfileEffect> = _effects.asSharedFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userManager.observeUser().first() ?: return@launch
            _state.update {
                it.copy(
                    displayAlias = user.displayAlias ?: "",
                    profileImageUrl = user.profileImageUrl,
                    name = user.name,
                    email = user.email
                )
            }
        }
    }

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.UpdateDisplayAlias -> {
                _state.update { it.copy(displayAlias = event.value) }
            }
            is EditProfileEvent.SaveProfile -> saveProfile()
            is EditProfileEvent.UploadAvatar -> uploadAvatar(event.imageBytes, event.mimeType)
            is EditProfileEvent.DeleteAvatar -> deleteAvatar()
            is EditProfileEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun saveProfile() {
        val currentState = _state.value
        val alias = currentState.displayAlias.trim()

        if (alias.isNotEmpty() && (alias.length < 2 || alias.length > 30)) {
            _state.update { it.copy(errorMessage = "Username must be 2-30 characters") }
            return
        }

        if (alias.isNotEmpty() && !alias.matches("^[a-zA-Z0-9 _-]+$".toRegex())) {
            _state.update { it.copy(errorMessage = "Only letters, numbers, spaces, underscores, and hyphens allowed") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            val aliasToSend = alias.ifEmpty { null }
            updateProfileUseCase(name = null, displayAlias = aliasToSend).fold(
                onSuccess = { updatedUser ->
                    userManager.setUser(updatedUser)
                    _state.update { it.copy(isSaving = false) }
                    _effects.emit(EditProfileEffect.ProfileSaved)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to update profile"
                        )
                    }
                }
            )
        }
    }

    private fun uploadAvatar(imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, errorMessage = null) }

            uploadAvatarUseCase(imageBytes, mimeType).fold(
                onSuccess = { url ->
                    _state.update { it.copy(isUploadingAvatar = false, profileImageUrl = url) }
                    // Update the user manager with new profile image
                    val currentUser = userManager.observeUser().first()
                    if (currentUser != null) {
                        userManager.setUser(currentUser.copy(profileImageUrl = url))
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = error.message ?: "Failed to upload avatar"
                        )
                    }
                }
            )
        }
    }

    private fun deleteAvatar() {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, errorMessage = null) }

            deleteAvatarUseCase().fold(
                onSuccess = {
                    _state.update { it.copy(isUploadingAvatar = false, profileImageUrl = null) }
                    val currentUser = userManager.observeUser().first()
                    if (currentUser != null) {
                        userManager.setUser(currentUser.copy(profileImageUrl = null))
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = error.message ?: "Failed to delete avatar"
                        )
                    }
                }
            )
        }
    }
}
