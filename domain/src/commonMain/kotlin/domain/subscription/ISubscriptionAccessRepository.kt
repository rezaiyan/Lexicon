package domain.subscription

import core.common.Try

/** Server-side premium state (the backend gate that AI and other server features enforce). */
interface ISubscriptionAccessRepository {
    /**
     * Asks the backend to reconcile premium with the store right now, instead of waiting for the
     * store → backend webhook. Feature access observers receive the result.
     */
    suspend fun syncWithServer(): Try<Unit>

    /** Re-reads premium from the backend if the cached value is stale. Keeps the old value on failure. */
    suspend fun refresh(): Try<Unit>
}
