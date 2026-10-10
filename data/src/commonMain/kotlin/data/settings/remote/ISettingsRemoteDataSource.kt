package data.settings.remote

import core.common.Try

interface ISettingsRemoteDataSource {
    suspend fun syncSettings(settings: SettingsSyncDto): Try<Unit>

    /** Reports the device's IANA timezone so the server times notifications for the user's day. */
    suspend fun syncTimezone(timezone: String): Try<Unit>
}
