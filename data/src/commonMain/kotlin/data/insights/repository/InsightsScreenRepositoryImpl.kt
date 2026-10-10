package data.insights.repository

import core.common.Try
import core.common.map
import data.insights.local.IInsightsScreenLocalDataSource
import data.insights.remote.IInsightsScreenRemoteDataSource
import data.insights.toDomain
import domain.insights.model.CachedInsights
import domain.insights.repository.IInsightsScreenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class InsightsScreenRepositoryImpl(
    private val remote: IInsightsScreenRemoteDataSource,
    private val local: IInsightsScreenLocalDataSource,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : IInsightsScreenRepository {

    override fun observe(): Flow<CachedInsights?> =
        local.observe().map { stored -> stored?.let { CachedInsights(it.dto.toDomain(), it.fetchedAtMs) } }

    // Try.map is inline and catches failures of the cache write, so the suspend call is safe here.
    override suspend fun refresh(timeZoneId: String): Try<Unit> =
        remote.fetch(timeZoneId).map { dto -> local.write(dto, nowMs()) }
}
