package domain.notifications.usecase

import core.common.Try
import core.common.UseCase
import domain.word.model.ProgressStats
import domain.notifications.repository.INotificationRepository
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.first

class ScheduleNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
    private val settingsRepository: ISettingsRepository
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
        val enabled = settingsRepository.getReviewRemindersEnabled().first()
        val minimumCards = settingsRepository.getMinimumDueCards()

        if (enabled && stats.dueCards >= minimumCards && !hasScheduled) {
            val title = titleProvider(stats.dueCards)
            val message = messageProvider(stats.dueCards)

            notificationRepository.scheduleReviewReminder(
                dueCount = stats.dueCards,
                title = title,
                message = message,
                delayMinutes = 24 * 60
            )

            hasScheduled = true
        }
    }
}
