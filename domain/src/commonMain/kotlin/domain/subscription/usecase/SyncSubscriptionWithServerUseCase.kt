package domain.subscription.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.subscription.ISubscriptionAccessRepository

/**
 * Pushes a fresh store purchase/restore to the backend so server-enforced premium (AI features,
 * daily insights) unlocks immediately. Safe to call repeatedly; the backend rate-limits it.
 */
class SyncSubscriptionWithServerUseCase(
    private val repository: ISubscriptionAccessRepository,
) : NoParamUseCase<Unit> {

    override suspend operator fun invoke(params: Unit): Try<Unit> = invoke()

    suspend operator fun invoke(): Try<Unit> = repository.syncWithServer()
}
