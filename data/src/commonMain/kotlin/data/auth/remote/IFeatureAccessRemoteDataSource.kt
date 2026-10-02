package data.auth.remote

import core.common.Try
import domain.auth.model.FeatureAccessResponse
import kotlinx.coroutines.flow.Flow

interface IFeatureAccessRemoteDataSource {
    fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse>

    /** Asks the backend to reconcile premium with the store; on success the cache is updated. */
    suspend fun syncWithStore(): Try<FeatureAccessResponse>

    /**
     * Refetches feature access unless the cached value is still fresh. On failure the cached
     * value is kept, so a flaky network never downgrades an open session.
     */
    suspend fun refresh(): Try<Unit>

    fun clearCache()
}
