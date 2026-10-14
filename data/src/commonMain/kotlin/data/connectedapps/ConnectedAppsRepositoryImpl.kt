package data.connectedapps

import core.common.Try
import core.common.map
import data.connectedapps.remote.ConnectedAppResponse
import data.connectedapps.remote.IConnectedAppsRemoteDataSource
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.model.ConnectedAppAccess
import domain.connectedapps.repository.IConnectedAppsRepository

class ConnectedAppsRepositoryImpl(
    private val remote: IConnectedAppsRemoteDataSource,
) : IConnectedAppsRepository {

    override suspend fun getConnectedApps(): Try<List<ConnectedApp>> =
        remote.getConnections().map { apps -> apps.map { it.toDomain() } }

    override suspend fun disconnect(id: String): Try<Unit> = remote.disconnect(id)
}

internal fun ConnectedAppResponse.toDomain() = ConnectedApp(
    id = id,
    name = clientName,
    access = scopes.mapNotNull(ConnectedAppAccess::fromScope).toSet(),
    connectedAtMillis = connectedAt,
)
