package fakes

import core.common.Try
import domain.insights.repository.IDismissedCoachCardsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeDismissedCoachCardsRepository : IDismissedCoachCardsRepository {
    val dismissed = MutableStateFlow<Set<String>>(emptySet())
    override fun observe(): Flow<Set<String>> = dismissed
    override suspend fun dismiss(cardId: String): Try<Unit> {
        dismissed.update { it + cardId }
        return Try.success(Unit)
    }
}
