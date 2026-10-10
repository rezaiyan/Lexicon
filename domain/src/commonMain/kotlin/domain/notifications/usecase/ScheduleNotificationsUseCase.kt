package domain.notifications.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.word.model.ProgressStats
import domain.notifications.repository.INotificationRepository
import domain.notifications.repository.IPushTokenRepository
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Schedules an on-device review reminder for a day from now. Only a fallback for devices the
 * server can't reach: once the push token is registered, the server sends due-card reminders at
 * the user's usual study hour (and skips days they already studied), so a local one would double up.
 */
class ScheduleNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
    private val settingsRepository: ISettingsRepository,
    private val pushTokenRepository: IPushTokenRepository,
) : UseCase<ScheduleNotificationsUseCase.Params, Unit> {
    private var hasScheduled: Boolean = false

    data class Params(
        val stats: ProgressStats,
        val titleProvider: (Int) -> String,
        val messageProvider: (Int) -> String
    )

    override suspend operator fun invoke(params: Params) =
        invoke(params.stats, params.titleProvider, params.messageProvider)

    suspend operator fun invoke(
        stats: ProgressStats,
        titleProvider: (Int) -> String,
        messageProvider: (Int) -> String
    ): Try<Unit> = Try {
        if (pushTokenRepository.isRegisteredWithServer()) return@Try

        val enabled = settingsRepository.getReviewRemindersEnabled().first()
        val minimumCards = settingsRepository.getMinimumDueCards().getOrThrow()

        if (enabled && stats.dueCards >= minimumCards && !hasScheduled) {
            val title = titleProvider(stats.dueCards)
            val message = messageProvider(stats.dueCards)

            notificationRepository.scheduleReviewReminder(
                dueCount = stats.dueCards,
                title = title,
                message = message,
                delayMinutes = 24 * 60
            ).getOrThrow()

            hasScheduled = true
        }
    }
}
