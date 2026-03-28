package domain.wordrush.repository

import core.common.Try
import domain.wordrush.model.WordRushGameRecord

interface IWordRushRecorder {
    suspend fun recordGame(game: WordRushGameRecord): Try<Unit>
    suspend fun retryPendingSync(): Try<Unit>
}
