package fakes

import core.common.Try
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.repository.IConnectedAppsRepository

class FakeConnectedAppsRepository(
    var apps: List<ConnectedApp> = emptyList(),
) : IConnectedAppsRepository {
    /** When set, every call fails with it. */
    var failure: Throwable? = null
    val disconnected = mutableListOf<String>()

    override suspend fun getConnectedApps(): Try<List<ConnectedApp>> =
        failure?.let { Try.failure(it) } ?: Try.success(apps)

    override suspend fun disconnect(id: String): Try<Unit> {
        failure?.let { return Try.failure(it) }
        disconnected += id
        apps = apps.filterNot { it.id == id }
        return Try.success(Unit)
    }
}
