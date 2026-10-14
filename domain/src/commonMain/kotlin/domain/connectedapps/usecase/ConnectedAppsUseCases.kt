package domain.connectedapps.usecase

import core.common.Try
import core.common.UseCase
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.repository.IConnectedAppsRepository

class GetConnectedAppsUseCase(
    private val repository: IConnectedAppsRepository,
) : UseCase<Unit, List<ConnectedApp>> {
    override suspend fun invoke(params: Unit): Try<List<ConnectedApp>> = repository.getConnectedApps()
}

/** Takes the app id. */
class DisconnectAppUseCase(
    private val repository: IConnectedAppsRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> = repository.disconnect(params)
}
