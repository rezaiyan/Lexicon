package fakes

import core.common.Try
import domain.subscription.ISubscriptionAccessRepository

class FakeSubscriptionAccessRepository : ISubscriptionAccessRepository {
    var result: Try<Unit> = Try.success(Unit)
    var syncCount = 0
        private set

    override suspend fun syncWithServer(): Try<Unit> {
        syncCount++
        return result
    }

    var refreshCount = 0
        private set
    var forcedRefreshCount = 0
        private set

    override suspend fun refresh(force: Boolean): Try<Unit> {
        refreshCount++
        if (force) forcedRefreshCount++
        return result
    }
}
