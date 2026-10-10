package data.insights

import data.insights.local.IInsightsScreenLocalDataSource
import data.insights.local.StoredInsightsScreen
import data.insights.remote.InsightsScreenDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [IInsightsScreenLocalDataSource]; lives here because `:test` does not depend on `:data`. */
class FakeInsightsScreenLocalDataSource : IInsightsScreenLocalDataSource {
    val stored = MutableStateFlow<StoredInsightsScreen?>(null)
    val dismissed = MutableStateFlow<Set<String>>(emptySet())
    var clearCount = 0
        private set

    override fun observe(): Flow<StoredInsightsScreen?> = stored

    override suspend fun write(dto: InsightsScreenDto, fetchedAtMs: Long) {
        stored.value = StoredInsightsScreen(dto, fetchedAtMs)
    }

    override fun observeDismissed(): Flow<Set<String>> = dismissed

    override suspend fun dismiss(cardId: String, today: String) {
        dismissed.value = dismissed.value.filterTo(mutableSetOf()) { it.endsWith(":$today") } + cardId
    }

    override suspend fun clear() {
        clearCount++
        stored.value = null
        dismissed.value = emptySet()
    }
}
