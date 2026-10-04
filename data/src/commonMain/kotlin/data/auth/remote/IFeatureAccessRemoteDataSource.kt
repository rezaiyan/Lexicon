package data.auth.remote

import core.common.Try
import domain.auth.model.FeatureAccessResponse
import kotlinx.coroutines.flow.Flow

interface IFeatureAccessRemoteDataSource {
    fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse>

    /** Asks the backend to reconcile premium with the store; on success the cache is updated. */
    suspend fun syncWithStore(): Try<FeatureAccessResponse>

    /**
     * Refetches feature access unless the cached value is still fresh, or always with [force]
     * (the server told us it changed). On failure the cached value is kept, so a flaky network
     * never downgrades an open session.
     */
    suspend fun refresh(force: Boolean = false): Try<Unit>

    /** Forgets the cached access in memory and on the device (logout, account deletion). */
    suspend fun clearCache()
}
