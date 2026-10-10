package domain.insights.repository

import core.common.Try
import domain.insights.model.CachedInsights
import kotlinx.coroutines.flow.Flow

interface IInsightsScreenRepository {
    /** Last cached screen (null before the first successful fetch); re-emits after each refresh. */
    fun observe(): Flow<CachedInsights?>

    /** Fetches a fresh screen computed in [timeZoneId] and replaces the cache. Failure keeps the cache. */
    suspend fun refresh(timeZoneId: String): Try<Unit>
}
