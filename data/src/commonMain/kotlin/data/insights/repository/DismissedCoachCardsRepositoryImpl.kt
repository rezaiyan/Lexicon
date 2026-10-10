package data.insights.repository

import core.common.Try
import data.insights.local.IInsightsScreenLocalDataSource
import domain.insights.repository.IDismissedCoachCardsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class DismissedCoachCardsRepositoryImpl(
    private val local: IInsightsScreenLocalDataSource,
    private val today: () -> String = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    },
) : IDismissedCoachCardsRepository {

    override fun observe(): Flow<Set<String>> = local.observeDismissed()

    override suspend fun dismiss(cardId: String): Try<Unit> = Try { local.dismiss(cardId, today()) }
}
