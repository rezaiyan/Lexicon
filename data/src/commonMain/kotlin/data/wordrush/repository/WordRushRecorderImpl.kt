package data.wordrush.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import core.common.Try
import data.core.database.LexiconQueries
import data.wordrush.toSyncRequest
import data.wordrush.remote.IWordRushDataSource
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.model.WordRushGrade
import domain.wordrush.repository.IWordRushRecorder
import expects.logNetwork

/**
 * Records Word Rush games to the local SQLDelight queue and attempts to sync to the backend.
 * Failed syncs remain in the queue for retry on next game or explicit retryPendingSync() call.
 */
class WordRushRecorderImpl(
    private val dataSource: IWordRushDataSource,
    private val queries: LexiconQueries,
) : IWordRushRecorder {

    override suspend fun recordGame(game: WordRushGameRecord): Try<Unit> {
        // 1. Insert to local queue so the game survives a crash before sync completes
        queries.insertWordRushPendingGame(
            client_game_id = game.clientGameId,
            score = game.score.toLong(),
            total_questions = game.totalQuestions.toLong(),
            correct_count = game.correctCount.toLong(),
            best_streak = game.bestStreak.toLong(),
            duration_ms = game.durationMs,
            avg_response_ms = game.avgResponseMs,
            grade = game.grade.code,
            lives_remaining = game.livesRemaining.toLong(),
            completed_normally = if (game.completedNormally) 1L else 0L,
            played_at = game.playedAt,
        )

        // 2. Attempt to sync all pending games (includes the one just inserted + prior failures)
        return syncAllPending()
    }

    override suspend fun retryPendingSync(): Try<Unit> = syncAllPending()

    private suspend fun syncAllPending(): Try<Unit> {
        val pending = queries.selectAllWordRushPendingGames().awaitAsList()
        if (pending.isEmpty()) return Try.success(Unit)

        val syncRequests = pending.map { row ->
            WordRushGameRecord(
                clientGameId = row.client_game_id,
                score = row.score.toInt(),
                totalQuestions = row.total_questions.toInt(),
                correctCount = row.correct_count.toInt(),
                bestStreak = row.best_streak.toInt(),
                durationMs = row.duration_ms,
                avgResponseMs = row.avg_response_ms,
                grade = WordRushGrade.fromCode(row.grade),
                livesRemaining = row.lives_remaining.toInt(),
                completedNormally = row.completed_normally != 0L,
                playedAt = row.played_at,
            ).toSyncRequest()
        }

        return dataSource.syncGames(syncRequests).let { result ->
            when (result) {
                is Try.Success -> {
                    queries.deleteAllWordRushPendingGames()
                    logNetwork("WordRushRecorder", "Synced ${pending.size} games to backend")
                    Try.success(Unit)
                }
                is Try.Failure -> {
                    logNetwork(
                        "WordRushRecorder",
                        "Failed to sync ${pending.size} games: ${result.throwable.message}. Will retry later.",
                    )
                    // Return success so the caller doesn't see an error — the game is safely queued
                    Try.success(Unit)
                }
            }
        }
    }
}
