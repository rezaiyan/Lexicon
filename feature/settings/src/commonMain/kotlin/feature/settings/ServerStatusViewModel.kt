package feature.settings

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.connectivity.model.ServerStatus
import domain.connectivity.usecase.CheckServerStatusUseCase
import domain.connectivity.usecase.ObserveServerStatusUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ServerStatusState(
    val status: ServerStatus = ServerStatus.Checking,
    /** A check the user asked for is running. */
    val retrying: Boolean = false,
)

/** The connection row in Settings: live while the screen is open, with a manual re-check. */
class ServerStatusViewModel(
    private val observeServerStatus: ObserveServerStatusUseCase,
    private val checkServerStatus: CheckServerStatusUseCase,
) : BaseViewModel<ServerStatusState, Nothing>() {

    override fun initialState() = ServerStatusState()

    init {
        viewModelScope.launch {
            observeServerStatus(Unit)
                .catch { /* keep the last status shown; the next check corrects it */ }
                .collect { status -> updateState { copy(status = status) } }
        }
    }

    fun retry() {
        if (currentState.retrying) return
        updateState { copy(retrying = true) }
        viewModelScope.launch {
            checkServerStatus(Unit).fold(
                onSuccess = { status -> updateState { copy(status = status, retrying = false) } },
                onFailure = { updateState { copy(retrying = false) } },
            )
        }
    }
}
