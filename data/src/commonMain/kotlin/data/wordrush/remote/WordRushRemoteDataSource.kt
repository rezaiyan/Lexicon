package data.wordrush.remote

import core.common.Try
import core.common.doOnFailure
import core.common.doOnSuccess
import data.core.network.client.ApiClient
import expects.logNetwork

class WordRushRemoteDataSource(
    private val apiClient: ApiClient,
) : IWordRushDataSource {

    override suspend fun syncGames(games: List<SyncWordRushGameRequest>): Try<Unit> =
        apiClient.postUnit("/word-rush/sync", body = SyncWordRushRequest(games = games))
            .doOnSuccess { logNetwork("WordRushRemote", "Synced ${games.size} games") }
            .doOnFailure { logNetwork("WordRushRemote", "Sync failed: ${it.message}") }

    override suspend fun getInsights(): Try<WordRushInsightsResponse> =
        apiClient.getNotNull("/word-rush/insights")
}
