package feature.profile

import androidx.lifecycle.viewModelScope
import domain.auth.manager.IUserManager
import core.common.fold
import core.error.toUserMessage
import domain.profile.model.AliasValidationResult
import domain.profile.usecase.UpdateProfileUseCase
import domain.profile.usecase.ValidateDisplayAliasUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import core.base.BaseViewModel

data class EditProfileState(
    val displayAlias: String = "",
    val name: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface EditProfileEffect {
    data object ProfileSaved : EditProfileEffect
}

class EditProfileViewModel(
    private val userManager: IUserManager,
    private val updateProfileUseCase: UpdateProfileUseCase,
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
                    name = user.name,
                    email = user.email
                )
            }
        }
    }

    fun updateDisplayAlias(value: String) {
        updateState { copy(displayAlias = value, errorMessage = null) }
    }

    fun dismissError() {
        updateState { copy(errorMessage = null) }
    }

    fun saveProfile() {
        if (currentState.isSaving) return

        val alias = currentState.displayAlias.trim()

        when (validateDisplayAliasUseCase(alias)) {
            AliasValidationResult.TooShort -> {
                updateState { copy(errorMessage = "Username must be 2-30 characters") }
                return
            }
            AliasValidationResult.TooLong -> {
                updateState { copy(errorMessage = "Username must be 2-30 characters") }
                return
            }
            AliasValidationResult.InvalidCharacters -> {
                updateState { copy(errorMessage = "Only letters, numbers, spaces, underscores, and hyphens allowed") }
                return
            }
            AliasValidationResult.Valid -> Unit
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
                    updateState { copy(isSaving = false, errorMessage = error.toUserMessage()) }
                }
            )
        }
    }

}
