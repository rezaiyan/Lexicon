package data.wordrush.remote

import core.common.Try

interface IWordRushDataSource {
    suspend fun syncGames(games: List<SyncWordRushGameRequest>): Try<Unit>
    suspend fun getInsights(): Try<WordRushInsightsResponse>
}
