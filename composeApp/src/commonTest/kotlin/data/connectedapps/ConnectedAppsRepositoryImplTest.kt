package data.connectedapps

import core.common.Try
import core.common.getOrNull
import data.connectedapps.remote.ConnectedAppResponse
import data.connectedapps.remote.IConnectedAppsRemoteDataSource
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.model.ConnectedAppAccess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectedAppsRepositoryImplTest {

    private val remote = object : IConnectedAppsRemoteDataSource {
        val disconnected = mutableListOf<String>()

        override suspend fun getConnections(): Try<List<ConnectedAppResponse>> = Try.success(
            listOf(ConnectedAppResponse("c1", "Claude", "claude.ai", listOf("words:read", "progress:read", "admin:all"), 1_000))
        )

        override suspend fun disconnect(id: String): Try<Unit> {
            disconnected += id
            return Try.success(Unit)
        }
    }
    private val repository = ConnectedAppsRepositoryImpl(remote)

    @Test
    fun `getConnectedApps maps scopes to access and drops unknown ones`() = runTest {
        val apps = repository.getConnectedApps().getOrNull()

        assertEquals(
            listOf(ConnectedApp("c1", "Claude", "claude.ai", setOf(ConnectedAppAccess.READ_WORDS, ConnectedAppAccess.READ_PROGRESS), 1_000)),
            apps,
        )
    }

    @Test
    fun `disconnect forwards the app id`() = runTest {
        repository.disconnect("c1")

        assertEquals(listOf("c1"), remote.disconnected)
    }
}
