package feature.settings

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.usecase.DisconnectAppUseCase
import domain.connectedapps.usecase.GetConnectedAppsUseCase
import kotlinx.coroutines.launch

data class ConnectedAppsState(
    /** Empty until loaded, and after a failed load: Settings shows the row only when there is something to manage. */
    val apps: List<ConnectedApp> = emptyList(),
    val disconnectingId: String? = null,
    /** App whose last disconnect attempt failed. */
    val failedId: String? = null,
)

/** AI apps connected over MCP: listed in Settings and revoked from there. */
class ConnectedAppsViewModel(
    private val getConnectedApps: GetConnectedAppsUseCase,
    private val disconnectApp: DisconnectAppUseCase,
) : BaseViewModel<ConnectedAppsState, Nothing>() {

    override fun initialState() = ConnectedAppsState()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            getConnectedApps(Unit).fold(
                onSuccess = { apps -> updateState { copy(apps = apps) } },
                onFailure = { /* keep what is shown; the row hides when nothing ever loaded */ },
            )
        }
    }

    fun disconnect(id: String) {
        if (currentState.disconnectingId != null) return
        updateState { copy(disconnectingId = id, failedId = null) }
        viewModelScope.launch {
            disconnectApp(id).fold(
                onSuccess = { updateState { copy(apps = apps.filterNot { it.id == id }, disconnectingId = null) } },
                onFailure = { updateState { copy(disconnectingId = null, failedId = id) } },
            )
        }
    }
}
