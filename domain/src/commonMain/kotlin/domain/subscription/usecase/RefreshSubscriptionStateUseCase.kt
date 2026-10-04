package domain.subscription.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.subscription.ISubscriptionAccessRepository
import domain.subscription.ISubscriptionManager

/**
 * Runs when the server says the user's subscription changed (store webhook → silent push):
 * re-reads premium from the backend and the store status from the store SDK, both bypassing
 * their caches. The push carries no data on purpose; this fetch is the source of truth.
 */
class RefreshSubscriptionStateUseCase(
    private val accessRepository: ISubscriptionAccessRepository,
    private val subscriptionManager: ISubscriptionManager,
) : NoParamUseCase<Unit> {

    override suspend operator fun invoke(params: Unit): Try<Unit> = invoke()

    /** Both sources are refreshed even if one fails; the first failure is reported. */
    suspend operator fun invoke(): Try<Unit> {
        val access = accessRepository.refresh(force = true)
        val store = subscriptionManager.refreshCustomerInfo()
        return when {
            access.isFailure -> access
            store is Try.Failure -> Try.failure(store.throwable)
            else -> Try.success(Unit)
        }
    }
}
