package feature.settings

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.common.getOrElse
import core.error.DomainError
import core.error.toUserMessage
import domain.passkey.model.Passkey
import domain.passkey.usecase.CreatePasskeyUseCase
import domain.passkey.usecase.DeletePasskeyUseCase
import domain.passkey.usecase.GetPasskeysUseCase
import domain.passkey.usecase.IsPasskeyAvailableUseCase
import kotlinx.coroutines.launch

data class PasskeysState(
    /** The device can create passkeys; Settings shows the row only then. */
    val available: Boolean = false,
    val passkeys: List<Passkey> = emptyList(),
    /** The domain the passkeys belong to, from the last list; deleting needs it to clean up the device. */
    val rpId: String? = null,
    val creating: Boolean = false,
    val deletingId: Long? = null,
    /** Why the last create or delete failed; cleared by the next attempt. */
    val error: String? = null,
)

/** Passkeys for signing in without Google or Apple: created, listed and deleted from Settings. */
class PasskeysViewModel(
    private val isPasskeyAvailable: IsPasskeyAvailableUseCase,
    private val getPasskeys: GetPasskeysUseCase,
    private val createPasskey: CreatePasskeyUseCase,
    private val deletePasskey: DeletePasskeyUseCase,
) : BaseViewModel<PasskeysState, Nothing>() {

    override fun initialState() = PasskeysState()

    init {
        viewModelScope.launch {
            val available = isPasskeyAvailable(Unit).getOrElse { false }
            updateState { copy(available = available) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            getPasskeys(Unit).fold(
                onSuccess = { account -> updateState { copy(passkeys = account.passkeys, rpId = account.rpId) } },
                onFailure = { /* keep what is shown; the sheet still offers to create one */ },
            )
        }
    }

    fun create() {
        if (currentState.creating) return
        updateState { copy(creating = true, error = null) }
        viewModelScope.launch {
            createPasskey(Unit).fold(
                onSuccess = { passkey -> updateState { copy(passkeys = passkeys + passkey, creating = false) } },
                onFailure = { error ->
                    val message = error.takeUnless { it is DomainError.Passkey.Cancelled }?.toUserMessage()
                    updateState { copy(creating = false, error = message) }
                },
            )
        }
    }

    fun delete(id: Long) {
        if (currentState.deletingId != null) return
        val passkey = currentState.passkeys.firstOrNull { it.id == id } ?: return
        updateState { copy(deletingId = id, error = null) }
        viewModelScope.launch {
            deletePasskey(DeletePasskeyUseCase.Params(passkey, currentState.rpId)).fold(
                onSuccess = { updateState { copy(passkeys = passkeys.filterNot { it.id == id }, deletingId = null) } },
                onFailure = { error -> updateState { copy(deletingId = null, error = error.toUserMessage()) } },
            )
        }
    }
}
