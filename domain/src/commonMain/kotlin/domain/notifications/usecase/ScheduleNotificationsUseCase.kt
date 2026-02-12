package domain.notifications.usecase

import domain.word.model.ProgressStats
import domain.notifications.repository.INotificationRepository
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.first

class ScheduleNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
    private val settingsRepository: ISettingsRepository
) {
    private var hasScheduled: Boolean = false
    
    suspend operator fun invoke(
        stats: ProgressStats,
        titleProvider: (Int) -> String,
        messageProvider: (Int) -> String
    ) {
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
