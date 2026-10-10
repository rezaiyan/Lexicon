package domain.insights.repository

import core.common.Try
import kotlinx.coroutines.flow.Flow

interface IDismissedCoachCardsRepository {
    fun observe(): Flow<Set<String>>
    suspend fun dismiss(cardId: String): Try<Unit>
}
