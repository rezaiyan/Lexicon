package domain.connectivity.repository

import core.common.Try
import domain.connectivity.model.ServerStatus
import kotlinx.coroutines.flow.Flow

interface IServerStatusRepository {
    /** Live status. Keeps checking the server while collected, so it changes without a refresh. */
    fun observeStatus(): Flow<ServerStatus>

    /** Checks the server now instead of waiting for the next scheduled check. */
    suspend fun check(): Try<ServerStatus>
}
