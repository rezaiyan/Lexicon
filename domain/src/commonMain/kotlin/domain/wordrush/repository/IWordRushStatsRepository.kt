package domain.wordrush.repository

import core.common.Try
import domain.wordrush.model.WordRushInsights

interface IWordRushStatsRepository {
    suspend fun getInsights(): Try<WordRushInsights>
}
