package domain.notifications.usecase

import core.common.Try
import core.common.UseCase
import domain.notifications.repository.INotificationEngagementRepository

/** Reports that the learner finished the comeback rescue session with id [params]. */
class ReportRescueCompletedUseCase(
    private val repository: INotificationEngagementRepository
) : UseCase<Long, Unit> {

    override suspend fun invoke(params: Long): Try<Unit> = repository.reportRescueCompleted(params)
}
