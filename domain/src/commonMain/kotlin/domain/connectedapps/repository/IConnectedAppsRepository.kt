package domain.connectedapps.repository

import core.common.Try
import domain.connectedapps.model.ConnectedApp

/** Grants live on the server only: there is nothing to show offline, so both calls go to the network. */
interface IConnectedAppsRepository {
    suspend fun getConnectedApps(): Try<List<ConnectedApp>>

    /** Revokes every grant given to app [id]; its access ends on its next request. */
    suspend fun disconnect(id: String): Try<Unit>
}
