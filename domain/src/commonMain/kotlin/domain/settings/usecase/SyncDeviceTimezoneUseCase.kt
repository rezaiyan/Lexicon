package domain.settings.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.settings.repository.ISettingsRepository

/**
 * Tells the server the device's timezone, so reminders and streak warnings arrive at sensible
 * local hours. Cheap to call on every app resume: nothing is sent while the zone is unchanged.
 */
class SyncDeviceTimezoneUseCase(
    private val settingsRepository: ISettingsRepository,
) : NoParamUseCase<Unit> {

    override suspend operator fun invoke(params: Unit): Try<Unit> = invoke()

    suspend operator fun invoke(): Try<Unit> = settingsRepository.syncDeviceTimezone()
}
