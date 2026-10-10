package domain.insights.usecase

import core.common.FlowUseCase
import core.common.Try
import core.common.UseCase
import domain.insights.model.CachedInsights
import domain.insights.repository.IDismissedCoachCardsRepository
import domain.insights.repository.IInsightsScreenRepository
import kotlinx.coroutines.flow.Flow

class ObserveInsightsScreenUseCase(
    private val repository: IInsightsScreenRepository,
) : FlowUseCase<Unit, CachedInsights?> {
    override fun invoke(params: Unit): Flow<CachedInsights?> = repository.observe()
}

/** [params] is the IANA time zone id the server should compute days and hours in. */
class RefreshInsightsScreenUseCase(
    private val repository: IInsightsScreenRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> = repository.refresh(params)
}

class ObserveDismissedCoachCardsUseCase(
    private val repository: IDismissedCoachCardsRepository,
) : FlowUseCase<Unit, Set<String>> {
    override fun invoke(params: Unit): Flow<Set<String>> = repository.observe()
}

class DismissCoachCardUseCase(
    private val repository: IDismissedCoachCardsRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> = repository.dismiss(params)
}
