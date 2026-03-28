package data.wordrush.repository

import core.common.Try
import core.common.map
import data.wordrush.remote.IWordRushDataSource
import data.wordrush.toDomain
import domain.wordrush.model.WordRushInsights
import domain.wordrush.repository.IWordRushStatsRepository

class WordRushStatsRepositoryImpl(
    private val dataSource: IWordRushDataSource,
) : IWordRushStatsRepository {
    override suspend fun getInsights(): Try<WordRushInsights> =
        dataSource.getInsights().map { it.toDomain() }
}
