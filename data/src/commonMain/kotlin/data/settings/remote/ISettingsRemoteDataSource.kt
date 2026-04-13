package data.settings.remote

import core.common.Try

interface ISettingsRemoteDataSource {
    suspend fun syncSettings(settings: SettingsSyncDto): Try<Unit>
}
