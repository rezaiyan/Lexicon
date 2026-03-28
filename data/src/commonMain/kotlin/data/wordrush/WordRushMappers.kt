package data.wordrush

import data.wordrush.remote.SyncWordRushGameRequest
import data.wordrush.remote.WordRushInsightsResponse
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.model.WordRushInsights

fun WordRushGameRecord.toSyncRequest(): SyncWordRushGameRequest = SyncWordRushGameRequest(
    clientGameId = clientGameId,
    score = score,
    totalQuestions = totalQuestions,
    correctCount = correctCount,
    bestStreak = bestStreak,
    durationMs = durationMs,
    avgResponseMs = avgResponseMs,
    grade = grade,
    livesRemaining = livesRemaining,
    completedNormally = completedNormally,
    playedAt = playedAt,
)

fun WordRushInsightsResponse.toDomain(): WordRushInsights = WordRushInsights(
    totalGames = totalGames,
    totalCompleted = totalCompleted,
    completionRatePercent = completionRatePercent,
    bestStreakEver = bestStreakEver,
    avgScore = avgScore,
    avgAccuracyPercent = avgAccuracyPercent,
    totalTimePlayedMs = totalTimePlayedMs,
    avgDurationMs = avgDurationMs,
    avgResponseMs = avgResponseMs,
)
