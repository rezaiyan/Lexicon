package data.settings.remote

import core.common.Try
import data.core.network.client.ApiClient

class SettingsRemoteDataSourceImpl(
    private val apiClient: ApiClient
) : ISettingsRemoteDataSource {
    override suspend fun syncSettings(settings: SettingsSyncDto): Try<Unit> =
        apiClient.patchUnit("/settings", body = settings)

    override suspend fun syncTimezone(timezone: String): Try<Unit> =
        apiClient.putUnit("/settings/timezone", body = TimezoneSyncDto(timezone))
}
