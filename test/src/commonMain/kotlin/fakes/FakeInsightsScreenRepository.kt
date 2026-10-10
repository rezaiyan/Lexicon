package fakes

import core.common.Try
import domain.insights.model.CachedInsights
import domain.insights.repository.IInsightsScreenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInsightsScreenRepository(initial: CachedInsights? = null) : IInsightsScreenRepository {
    val cache = MutableStateFlow(initial)
    var nextRefresh: CachedInsights? = null
    var refreshError: Throwable? = null
    val refreshedZones = mutableListOf<String>()

    override fun observe(): Flow<CachedInsights?> = cache

    override suspend fun refresh(timeZoneId: String): Try<Unit> {
        refreshedZones += timeZoneId
        refreshError?.let { return Try.failure(it) }
        nextRefresh?.let { cache.value = it }
        return Try.success(Unit)
    }
}
