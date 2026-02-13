package data.settings.remote

import data.core.network.client.ApiClient
import data.settings.remote.model.RemoteSettings
import domain.common.Try
import domain.common.getOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Remote data source for user settings operations
 * Handles fetching and updating user settings from the backend
 */
class SettingsRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun getSettings(): Try<RemoteSettings> =
        apiClient.getNotNull<RemoteSettings>("/settings")

    suspend fun updateSettings(settings: RemoteSettings): Try<RemoteSettings> =
        apiClient.patchNotNull(
            path = "/settings",
            body = settings
        )

    /**
     * Flow-based wrappers to avoid exposing Try types to callers.
     * Emits only on success; failures complete without emission.
     */
    fun getSettingsAsFlow(): Flow<RemoteSettings> = flow {
        val result = getSettings()
        result.getOrNull()?.let { emit(it) }
    }

    fun updateSettingsAsFlow(settings: RemoteSettings): Flow<RemoteSettings> = flow {
        val result = updateSettings(settings)
        result.getOrNull()?.let { emit(it) }
    }
}
